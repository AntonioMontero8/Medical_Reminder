package com.example.medicalreminder.ui.recetas

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.medicalreminder.ui.theme.MRColors
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.launch

private const val GEMINI_API_KEY = "api_key_gemini"

@Composable
fun PantallaCargarReceta(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var imageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var respuesta by remember { mutableStateOf("El resultado JSON aparecerá aquí...") }
    var isLoading by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            imageBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, it)
                ImageDecoder.decodeBitmap(source)
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, it)
            }
        }
    }

    val generativeModel = remember {
        GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = GEMINI_API_KEY,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
            }
        )
    }

    val prompt = """
        Eres un asistente médico experto en leer recetas médicas.
        Analiza la imagen adjunta y extrae el tratamiento a seguir.
        Devuelve ÚNICAMENTE un objeto JSON válido con la siguiente estructura, sin texto adicional ni bloques de código (```):
        {
          "medicamentos": [
            {
              "nombre": "Nombre del medicamento",
              "dosis": "Cantidad a tomar (ej. 1 tableta, 500mg)",
              "horario": "Frecuencia u horario (ej. cada 8 horas)"
            }
          ],
          "indicacionesGenerales": "Cualquier otra nota, dieta o cuidado mencionado. Nulo si no hay."
        }
        Si no puedes leer un dato por la letra del médico, pon "No legible" en ese campo.
    """.trimIndent()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MRColors.Background)
            .systemBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Encabezado estilizado
        Text(
            text = "Escanear receta",
            fontSize = 32.sp,
            lineHeight = 38.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MRColors.Navy,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Toma o sube una foto de tu prescripción médica para extraer tus medicamentos automáticamente.",
            fontSize = 16.sp,
            color = MRColors.TextGray,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))

        // Botón principal de acción
        Button(
            onClick = { imagePickerLauncher.launch("image/*") },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MRColors.Navy)
        ) {
            Icon(Icons.Outlined.DocumentScanner, contentDescription = null, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Text("SELECCIONAR IMAGEN", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Vista previa de la imagen con diseño de tarjeta
        imageBitmap?.let { bitmap ->
            Surface(
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(2.5.dp, MRColors.Navy),
                color = MRColors.Background,
                modifier = Modifier.fillMaxWidth()
            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Receta seleccionada",
                    modifier = Modifier
                        .height(200.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp)),
                    contentScale = ContentScale.Crop
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Botón de extracción
        Button(
            onClick = {
                if (imageBitmap != null) {
                    isLoading = true
                    respuesta = "Analizando receta... \nPor favor espera."
                    coroutineScope.launch {
                        try {
                            val inputContent = content {
                                image(imageBitmap!!)
                                text(prompt)
                            }
                            val response = generativeModel.generateContent(inputContent)
                            respuesta = response.text ?: "No hubo respuesta"
                        } catch (e: Exception) {
                            respuesta = "Hubo un error: ${e.message}"
                        } finally {
                            isLoading = false
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isLoading) MRColors.TextGray else MRColors.Navy
            ),
            enabled = imageBitmap != null && !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = MRColors.Background, modifier = Modifier.size(24.dp))
            } else {
                Icon(Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(12.dp))
                Text("EXTRAER DATOS", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Contenedor de resultados estilizado tipo formulario/tarjeta
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MRColors.FieldBg,
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            Text(
                text = respuesta,
                fontSize = 15.sp,
                color = MRColors.Navy,
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            )
        }
    }
}