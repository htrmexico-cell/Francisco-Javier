package com.example.data

import android.util.Log
import com.example.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

data class NewsItem(
    val id: String,
    val title: String,
    val category: String, // "Liga MX", "Selección Nacional", "Internacional"
    val summary: String,
    val body: String,
    val timeAgo: String,
    val isBreaking: Boolean,
    val source: String,
    val graphicType: String // "stadium", "jersey", "ball", "trophy", "training"
)

data class SportsNewsData(
    val summaryOfTheDay: String,
    val tickerHeadlines: List<String>,
    val articles: List<NewsItem>
)

object NewsRepository {
    private const val TAG = "NewsRepository"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // High quality fallback news for June 8, 2026
    val fallbackData = SportsNewsData(
        summaryOfTheDay = "Inicio de semana intenso en el fútbol mexicano y mundial. En la Liga MX, el mercado de transferencias para el Apertura 2026 está que arde con Chivas y América liderando los rumores de fichajes bomba. Por su parte, la Selección Mexicana se reporta lista en California afinando los últimos detalles tácticos antes de encarar su participación continental veraniega. En el plano internacional, el Real Madrid sigue acaparando reflectores tras ganar la Copa de Europa y delinear su nuevo plantel estelar de cara a la pretemporada.",
        tickerHeadlines = listOf(
            "🔴 ÚLTIMA HORA: ¡Fichaje bomba! Chivas abre pláticas formales por Orbelín Pineda para el Apertura 2026.",
            "🦅 SELECCIÓN: Jaime Lozano confirma recorte final; tres jugadores abandonan la concentración del Tri.",
            "⚽ INTERNACIONAL: Kylian Mbappé reporta con el Real Madrid y lucirá el histórico dorsal 9."
        ),
        articles = listOf(
            NewsItem(
                id = "1",
                title = "¡Bombazo! Chivas busca el regreso de Orbelín Pineda para el Apertura 2026",
                category = "Liga MX",
                summary = "La directiva rojiblanca ya inició gestiones para repatriar al 'Maguito' en un movimiento clave.",
                body = "Guadalajara, Jalisco.- Las Chivas Rayadas del Guadalajara quieren romper el mercado del Apertura 2026. Fuentes cercanas a la directiva tapatía confirman que Amaury Vergara y el comité deportivo han iniciado pláticas formales con el entorno de Orbelín Pineda para gestionar su regreso a la Liga MX.\n\nTras su aventura en Grecia con el AEK de Atenas, el mediocampista de la Selección Nacional vería con buenos ojos un regreso al club donde fue campeón. La negociación no será sencilla debido al costo de salida y su sueldo, pero el Rebaño Sagrado está dispuesto a hacer un esfuerzo económico histórico para consolidar su mediocampo y regalarle una alegría enorme a su afición.",
                timeAgo = "Hace 15 min",
                isBreaking = true,
                source = "Corresponsal Guadalajara",
                graphicType = "jersey"
            ),
            NewsItem(
                id = "2",
                title = "México define su parado táctico defensivo de cara al debut continental",
                category = "Selección Nacional",
                summary = "Jaime Lozano trabaja a doble sesión en San José, California, para ajustar la línea defensiva.",
                body = "San José, California.- La Selección Nacional de México afina los últimos detalles en su campamento de entrenamiento en territorio estadounidense. El estratega Jaime Lozano ordenó un entrenamiento a puerta cerrada centrándose exclusivamente en la coordinación defensiva, un área que provocó críticas en los últimos juegos amistosos.\n\nJohan Vásquez y César 'Cachorro' Montes liderarán la zaga central, mientras que en las bandas se perfilan Jorge Sánchez y Gerardo Arteaga. El 'Jimmy' sabe que la solidez defensiva será crucial para aspirar a grandes cosas, especialmente al enfrentar a delanteros de jerarquía internacional de Sudamérica en esta importante justa veraniega de 2026.",
                timeAgo = "Hace 1 hora",
                isBreaking = false,
                source = "Enviado Especial TUDN",
                graphicType = "training"
            ),
            NewsItem(
                id = "3",
                title = "El Real Madrid presenta su proyecto deportivo y confirma dorsal de Mbappé",
                category = "Internacional",
                summary = "La entidad blanca vive días históricos tras reinar en Europa y alistar la pretemporada.",
                body = "Madrid, España.- Tras consagrarse campeón en el máximo torneo de clubes de Europa, Florentino Pérez no descansa. El Real Madrid anunció de forma oficial la reestructuración de su plantilla comercial y confirmó que su nuevo fichaje galáctico, Kylian Mbappé, vestirá el mítico dorsal número '9' que dejara vacante Karim Benzema y que temporalmente no tenía dueño estelar.\n\nLas tiendas oficiales del club madrileño en el Bernabéu y la Gran Vía registraron filas kilométricas de aficionados entusiasmados por adquirir el jersey del astro francés. El conjunto de Carlo Ancelotti planea iniciar su gira estadounidense en julio, donde se enfrentará al Barcelona y al Milan en partidos de preparación.",
                timeAgo = "Hace 2 horas",
                isBreaking = false,
                source = "Agencias Madrid",
                graphicType = "trophy"
            ),
            NewsItem(
                id = "4",
                title = "Anselmi exige refuerzos rápidos para Cruz Azul tras arrancar pretemporada",
                category = "Liga MX",
                summary = "La Máquina inició entrenamientos en La Noria y busca cerrar un delantero sudamericano.",
                body = "Ciudad de México.- Martín Anselmi, timonel de Cruz Azul, dejó en claro en conferencia de prensa que la directiva celeste debe apurar las negociaciones para firmar al centro delantero estrella que tanto han buscado. Tras reportar a las pruebas físicas en La Noria, el estratega argentino enfatizó que contar con el plantel armado desde la pretemporada de este Apertura 2026 aumentará las probabilidades de conseguir la anhelada décima copa.\n\nSe especula que existen tres candidatos en Sudamérica, destacando un ariete uruguayo que milita en el fútbol argentino y un extremo brasileño de gran velocidad. Los aficionados celestes exigen resultados inmediatos tras quedarse muy cerca del título el semestre pasado.",
                timeAgo = "Hace 3 horas",
                isBreaking = false,
                source = "Corresponsal CDMX",
                graphicType = "stadium"
            ),
            NewsItem(
                id = "5",
                title = "Santiago Giménez despierta interés en la Serie A italiana tras su cuota goleadora",
                category = "Selección Nacional",
                summary = "El 'Bebote' está en la mira de dos gigantes italianos para reforzar su ataque este verano.",
                body = "Milán, Italia.- El gran momento de Santiago Giménez no pasa desapercibido en el viejo continente. Reportes de la 'Gazzetta dello Sport' señalan que tanto el AC Milan como la Juventus de Turín han solicitado información oficial sobre las condiciones contractuales del delantero mexicano, actual referente ofensivo del Feyenoord de Róterdam.\n\nEl club de los Países Bajos habría tasado al atacante azteca en cerca de 45 millones de euros, una cifra alta pero justa considerando su juventud y su consistencia goleadora en competiciones continentales. Un buen desempeño con el Tri en este verano podría terminar de catapultar la transferencia de Santi a una de las ligas más importantes del mundo.",
                timeAgo = "Hace 4 horas",
                isBreaking = false,
                source = "Sport Mediaset",
                graphicType = "ball"
            )
        )
    )

    fun fetchNewsFromGemini(): SportsNewsData {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w(TAG, "Gemini API Key is empty or placeholder. Returning high-quality fallback sports news.")
            return fallbackData
        }

        val prompt = """
            Eres el redactor jefe de deportes de un canal deportivo moderno mexicano de prestigio (estilo TUDN / ESPN).
            Hoy es lunes 8 de junio de 2026.
            
            Tu tarea es generar las noticias de fútbol del momento de forma sumamente realista, emocionante y profesional.
            
            Por favor, genera un objeto JSON en español con la estructura que se indica abajo. Usa fechas y contextos coherentes con el verano de junio de 2026 (por ejemplo: preparación y concentración de la Selección Mexicana para sus retos veraniegos de Copa América o similares, novedades de transferencias calientes o pretemporada de la Liga MX de cara al torneo Apertura 2026, mercado europeo o competiciones de verano).
            
            Estructura JSON requerida:
            {
              "summaryOfTheDay": "Un párrafo breve (mínimo 3 frases, máximo 5) que resume de forma emocionante las noticias y el estado de ánimo deportivo de hoy lunes 8 de junio de 2026 de forma integrada, ideal para un boletín matutino.",
              "tickerHeadlines": [
                "Un titular de última hora breve y sumamente urgente en formato de Ticker (debe iniciar con emoji alarmante, máximo 3 strings en total)"
              ],
              "articles": [
                {
                  "id": "Un número único como string (por ej: \"1\", \"2\")",
                  "title": "Un titular llamativo, deportivo y con estilo de periodista profesional de ESPN o TUDN",
                  "category": "Una de estas tres categorías exactas: 'Liga MX', 'Selección Nacional', 'Internacional'",
                  "summary": "Un resumen de una oración que enganche al usuario para leer la nota entera",
                  "body": "El cuerpo completo del artículo redactado de forma detallada, emocionante y profesional. Debe contener al menos de 2 a 3 párrafos separados por saltos de línea (\\n\\n). Incluye declaraciones ficticias pero sumamente realistas de los protagonistas, datos de canchas y análisis táctico o de mercado de fichajes.",
                  "timeAgo": "Un string de tiempo transcurrido realista como 'Hace 5 min', 'Hace 20 min', 'Hace 1 hora', o 'Hace 3 horas'",
                  "isBreaking": true o false (si es de suma prioridad para llamar la atención en la UI de inmediato)",
                  "source": "Una firma periodística atractiva (ej: 'Staff GOL MX', 'Enviado Especial TUDN', 'Corresponsal Noria')",
                  "graphicType": "Escribe una de estas opciones exactas para su iconografía o diseño visual: 'stadium', 'jersey', 'ball', 'trophy' o 'training'"
                }
              ]
            }

            Asegúrate de generar un mínimo de 5 artículos en total, distribuidos equitativamente en las tres categorías (ej. dos de Liga MX, dos de Selección Nacional, uno de Internacional).
            Respeta estrictamente el formato JSON y no agregues explicaciones fuera de él. Solo regresa el puro string JSON válido.
        """.trimIndent()

        val mediaType = "application/json; charset=utf-8".toMediaType()
        
        // Construct the payload for generateContent with system instruction and JSON output request
        val requestJson = JSONObject()
        val contentsArray = JSONArray()
        val contentObject = JSONObject()
        val partsArray = JSONArray()
        val partObject = JSONObject()
        
        partObject.put("text", prompt)
        partsArray.put(partObject)
        contentObject.put("parts", partsArray)
        contentsArray.put(contentObject)
        requestJson.put("contents", contentsArray)

        // Adding JSON mimeType in generationConfig to force JSON output
        val generationConfig = JSONObject()
        generationConfig.put("responseMimeType", "application/json")
        requestJson.put("generationConfig", generationConfig)

        val requestBody = requestJson.toString().toRequestBody(mediaType)
        
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "API request failed with code: ${response.code}. Serving fallbacks.")
                    return fallbackData
                }
                val bodyString = response.body?.string() ?: return fallbackData
                
                // Parse the Gemini API generateContent response
                val mainResponseObj = JSONObject(bodyString)
                val candidatesArray = mainResponseObj.getJSONArray("candidates")
                val firstCandidate = candidatesArray.getJSONObject(0)
                val responseContent = firstCandidate.getJSONObject("content")
                val responseParts = responseContent.getJSONArray("parts")
                var rawJsonString = responseParts.getJSONObject(0).getString("text").trim()
                
                // Clean the response string if it contains markdown markers
                if (rawJsonString.contains("```json")) {
                    rawJsonString = rawJsonString.substringAfter("```json").substringBefore("```")
                } else if (rawJsonString.contains("```")) {
                    rawJsonString = rawJsonString.substringAfter("```").substringBefore("```")
                }
                rawJsonString = rawJsonString.trim()

                return parseSportsNewsJson(rawJsonString)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching from Gemini API: ${e.message}", e)
            return fallbackData
        }
    }

    private fun parseSportsNewsJson(jsonStr: String): SportsNewsData {
        try {
            val root = JSONObject(jsonStr)
            val summaryOfTheDay = root.optString("summaryOfTheDay", fallbackData.summaryOfTheDay)
            
            val tickerArray = root.optJSONArray("tickerHeadlines")
            val tickerHeadlines = mutableListOf<String>()
            if (tickerArray != null) {
                for (i in 0 until tickerArray.length()) {
                    tickerHeadlines.add(tickerArray.getString(i))
                }
            } else {
                tickerHeadlines.addAll(fallbackData.tickerHeadlines)
            }

            val articlesArray = root.optJSONArray("articles")
            val articlesList = mutableListOf<NewsItem>()
            if (articlesArray != null) {
                for (i in 0 until articlesArray.length()) {
                    val articleObj = articlesArray.getJSONObject(i)
                    articlesList.add(
                        NewsItem(
                            id = articleObj.optString("id", i.toString()),
                            title = articleObj.optString("title", "Titular no disponible"),
                            category = articleObj.optString("category", "Liga MX"),
                            summary = articleObj.optString("summary", "Sin resumen"),
                            body = articleObj.optString("body", "Sin detalles adicionales."),
                            timeAgo = articleObj.optString("timeAgo", "Hace unos momentos"),
                            isBreaking = articleObj.optBoolean("isBreaking", false),
                            source = articleObj.optString("source", "Staff GOL MX"),
                            graphicType = articleObj.optString("graphicType", "ball")
                        )
                    )
                }
            } else {
                articlesList.addAll(fallbackData.articles)
            }

            return SportsNewsData(summaryOfTheDay, tickerHeadlines, articlesList)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Gemini JSON response, falling back. Content text: $jsonStr", e)
            return fallbackData
        }
    }
}
