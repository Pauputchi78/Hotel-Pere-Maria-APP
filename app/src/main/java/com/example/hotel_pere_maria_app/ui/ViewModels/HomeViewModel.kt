package com.example.hotel_pere_maria_app.ui.ViewModels

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hotel_pere_maria_app.ui.Models.Reservation
import com.example.hotel_pere_maria_app.ui.Models.ReservationRepository
import com.example.hotel_pere_maria_app.ui.Navegation.Routes
import com.example.hotel_pere_maria_app.ui.Service.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeViewModel: ViewModel() {

    private  val _navigationEvent = Channel<String>()
    val navigationEvent = _navigationEvent.receiveAsFlow()
    val listMisReservas = ReservationRepository.reservations
    private val _uiState = MutableStateFlow(HomeState())
    val uiState : StateFlow<HomeState> = _uiState

    private val _uiEvent = Channel<HomeUiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    private val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    fun descargarYGuardarFactura(context: Context, reservationId: String) {
        viewModelScope.launch {
            try {
                // Llamamos a la API
                val respuestaApi = RetrofitClient.reservationService.descargarFacturaPdf(reservationId)

                if (respuestaApi.isSuccessful) {
                    // Extraemos el cuerpo binario (los bytes del PDF) de la respuesta
                    val cuerpoDelPdf: ResponseBody? = respuestaApi.body()

                    if (cuerpoDelPdf != null) {
                        // Le pasamos los bytes que nos dio la API para que los escriba en el archivo .pdf
                        val archivoGuardado = guardarFacturaEnDisco(context, cuerpoDelPdf, reservationId)

                        if (archivoGuardado != null) {
                            Log.d("PDF", "Factura guardada en: ${archivoGuardado.absolutePath}")
                            abrirPdf(context, archivoGuardado)

                        } else {
                            Log.e("PDF", "Error al escribir los bytes en el almacenamiento")
                        }
                    }
                } else {
                    Log.e("API", "El servidor de Node devolvió un error: ${respuestaApi.code()}")
                }
            } catch (e: Exception) {
                Log.e("API", "Error de red o conexión: ${e.message}")
            }
        }
    }

    suspend fun guardarFacturaEnDisco(context: Context, body: ResponseBody, reservation_id: String): File? {
        return withContext(Dispatchers.IO) {
            try {
                // Creamos un archivo temporal en la carpeta de descargas de la app
                val file = File(context.getExternalFilesDir(null), "Factura_$reservation_id.pdf")

                var inputStream: InputStream? = null
                var outputStream: FileOutputStream? = null

                try {
                    val fileReader = ByteArray(4096)
                    inputStream = body.byteStream()
                    outputStream = FileOutputStream(file)

                    while (true) {
                        val read = inputStream.read(fileReader)
                        if (read == -1) break
                        outputStream.write(fileReader, 0, read)
                    }

                    outputStream.flush()
                    file // Devolvemos el archivo ya escrito con éxito
                } catch (e: Exception) {
                    null
                } finally {
                    inputStream?.close()
                    outputStream?.close()
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    fun abrirPdf(context: Context, archivo: File) {
        try {
            val uriSegura: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                archivo
            )

            // 2. Creamos el Intent con la acción de VER (ACTION_VIEW)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uriSegura, "application/pdf")
                // Concedemos permiso de lectura temporal a la app que vaya a abrir el PDF
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                // Si lo llamamos fuera de una Activity (como desde el ViewModel), necesitamos este flag
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            // 3. Arrancamos la actividad
            context.startActivity(intent)

        } catch (e: Exception) {
            // Si el móvil no tiene NINGUNA app instalada que pueda leer PDFs, saltará este error
            Toast.makeText(context, "No tienes ninguna aplicación instalada para abrir archivos PDF", Toast.LENGTH_LONG).show()
        }
    }

    fun onEditarReservaClick(id:String, reserva: Reservation) {
        if(reserva.cancelation_date!= null){
            _uiState.update { it.copy(mensajeRespuesta = "No es posible editar una reserva cancelada", errorRespusta = true) }
        }else{
            viewModelScope.launch {
                _navigationEvent.send("${Routes.ModReserva.route}/$id")
            }
        }

    }

    val proximaReserva: StateFlow<Reservation?> = listMisReservas
        .map { lista ->
            val hoy = Date()

            lista.filter {reserva ->
                reserva.check_in >= hoy && reserva.cancelation_date == null
            }.minByOrNull { it.check_in }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        cargarDatos()
    }

    private fun cargarDatos(){
        viewModelScope.launch {
            ReservationRepository.fetchReservations()
        }
    }
    fun limpiarMensaje(){
        _uiState.update { it.copy(mensajeRespuesta = null, errorRespusta = false) }
    }

    fun abrirMapa() {
        viewModelScope.launch {
            _uiEvent.send(HomeUiEvent.OpenMap("geo:0,0?q=IES+Pere+Maria+Orts+i+Bosch+Benidorm"))
        }
    }

    fun llamarHotel() {
        viewModelScope.launch {
            _uiEvent.send(HomeUiEvent.MakeCall("tel:965000000"))
        }
    }
    fun enviarCorreoHotel() {
        viewModelScope.launch {
            _uiEvent.send(
                HomeUiEvent.SendEmail(
                    address = "info@hotelperemaria.com",
                    subject = "Consulta desde la App Móvil"
                )
            )
        }
    }

}

data class HomeState(
    val mensajeRespuesta:String? = null,
    val errorRespusta: Boolean = false
)

sealed class HomeUiEvent {
    data class MakeCall(val uri: String) : HomeUiEvent()
    data class OpenMap(val uri: String) : HomeUiEvent()
    data class SendEmail(val address: String, val subject: String) : HomeUiEvent()
}