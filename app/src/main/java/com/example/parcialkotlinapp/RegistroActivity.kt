package com.example.parcialkotlinapp

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.parcialkotlinapp.api.ApiClient
import com.example.parcialkotlinapp.models.CrearUsuarioRequest
import com.example.parcialkotlinapp.models.Usuario
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Calendar

class RegistroActivity : AppCompatActivity() {

    private var fechaNacimientoSeleccionada: String = ""
    private var edadCalculada: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_registro)

        // COMPONENTES
        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val etNombre = findViewById<EditText>(R.id.etNombre)
        val etApellido = findViewById<EditText>(R.id.etApellido)
        val etCorreo = findViewById<EditText>(R.id.etCorreoRegistro)
        val etPasswordRegistro = findViewById<EditText>(R.id.etPasswordRegistro)
        val etFechaNacimiento = findViewById<EditText>(R.id.etFechaNacimiento)
        val etUniversidad = findViewById<EditText>(R.id.etUniversidad)
        val spSemestre = findViewById<Spinner>(R.id.spSemestre)
        val btnRegistrar = findViewById<Button>(R.id.btnRegistrar)
        val tvMensaje = findViewById<TextView>(R.id.tvMensajeRegistro)

        val semestres = arrayOf(
            "Seleccione semestre",
            "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12"
        )
        val adaptadorSemestre = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            semestres
        )
        adaptadorSemestre.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )
        spSemestre.adapter = adaptadorSemestre

        // BOTÓN FLECHA ATRÁS (SUPERIOR IZQUIERDA)
        btnBack.setOnClickListener {
            finish()
        }

        // DATE PICKER PARA FECHA DE NACIMIENTO
        etFechaNacimiento.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(
                this,
                { _, selectedYear, selectedMonth, selectedDay ->
                    // Formato YYYY-MM-DD
                    val formattedMonth = String.format("%02d", selectedMonth + 1)
                    val formattedDay = String.format("%02d", selectedDay)
                    fechaNacimientoSeleccionada = "$selectedYear-$formattedMonth-$formattedDay"
                    etFechaNacimiento.setText(fechaNacimientoSeleccionada)

                    // Calcular edad considerando día, mes y año
                    edadCalculada = calcularEdad(selectedYear, selectedMonth, selectedDay)
                },
                year,
                month,
                day
            )
            datePickerDialog.show()
        }

        // BOTÓN REGISTRAR
        btnRegistrar.setOnClickListener {
            val nombre = etNombre.text.toString().trim()
            val apellido = etApellido.text.toString().trim()
            val correo = etCorreo.text.toString().trim()
            val password = etPasswordRegistro.text.toString().trim()
            val universidad = etUniversidad.text.toString().trim()

            // VALIDACIONES
            if (nombre.isBlank()) {
                etNombre.error = "Ingrese el nombre"
                return@setOnClickListener
            }

            if (apellido.isBlank()) {
                etApellido.error = "Ingrese el apellido"
                return@setOnClickListener
            }

            if (correo.isBlank()) {
                etCorreo.error = "Ingrese el correo"
                return@setOnClickListener
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
                etCorreo.error = "Correo no válido"
                return@setOnClickListener
            }

            if (password.isBlank()) {
                etPasswordRegistro.error = "Ingrese la contraseña"
                return@setOnClickListener
            }

            if (fechaNacimientoSeleccionada.isBlank()) {
                etFechaNacimiento.error = "Seleccione la fecha de nacimiento"
                return@setOnClickListener
            }

            if (universidad.isBlank()) {
                etUniversidad.error = "Ingrese la universidad"
                return@setOnClickListener
            }

            if (spSemestre.selectedItemPosition == 0) {
                Toast.makeText(this, "Seleccione un semestre válido (1-10)", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            val semestre = spSemestre.selectedItem.toString().toInt()

            // OBJETO QUE ENVIAREMOS AL BACKEND

            val nuevoUsuario = CrearUsuarioRequest(
                nombre = nombre,
                apellido = apellido,
                correo = correo,
                password = password,
                edad = edadCalculada,
                fechaNacimiento = fechaNacimientoSeleccionada,
                universidad = universidad,
                semestre = semestre
            )

            tvMensaje.text = "Registrando usuario..."
            btnRegistrar.isEnabled = false

            // LLAMADA AL BACKEND
            ApiClient.usuarioApi
                .crearUsuario(nuevoUsuario)
                .enqueue(object : Callback<Usuario> {

                    override fun onResponse(
                        call: Call<Usuario>,
                        response: Response<Usuario>
                    ) {
                        btnRegistrar.isEnabled = true

                        if (response.isSuccessful) {
                            tvMensaje.text = ""

                            // 4.2 MOSTRAR DIÁLOGO EMERGENTE DE ÉXITO
                            val builder = AlertDialog.Builder(this@RegistroActivity)
                            builder.setTitle("¡Registro exitoso!")
                            builder.setMessage("El usuario ha sido registrado correctamente.")
                            builder.setPositiveButton("ACEPTAR") { dialog, _ ->
                                dialog.dismiss()
                                // Regresar al Login
                                finish()
                            }
                            builder.setCancelable(false)
                            builder.show()

                        } else {
                            // 4.3 MANEJO DE ERRORES DEL BACKEND (Ej. HTTP 400 con JSON de mensaje)
                            val errorBody = response.errorBody()?.string()
                            val errorMessage = if (!errorBody.isNullOrBlank()) {
                                try {
                                    val jsonObject = JSONObject(errorBody)
                                    jsonObject.optString(
                                        "mensaje",
                                        jsonObject.optString(
                                            "message",
                                            "Error del servidor: ${response.code()}"
                                        )
                                    )
                                } catch (e: Exception) {
                                    "Error del servidor: ${response.code()}"
                                }
                            } else {
                                "Error del servidor: ${response.code()}"
                            }

                            tvMensaje.text = errorMessage
                        }
                    }

                    override fun onFailure(
                        call: Call<Usuario>,
                        t: Throwable
                    ) {
                        btnRegistrar.isEnabled = true
                        tvMensaje.text = "Error de conexión: ${t.message}"
                    }
                })
        }
    }

    // CÁLCULO DE EDAD (DÍA, MES Y AÑO)
    private fun calcularEdad(year: Int, month: Int, day: Int): Int {
        val birthDate = Calendar.getInstance().apply { set(year, month, day) }
        val currentDate = Calendar.getInstance()
        var age = currentDate.get(Calendar.YEAR) - birthDate.get(Calendar.YEAR)

        if (currentDate.get(Calendar.MONTH) < birthDate.get(Calendar.MONTH) ||
            (currentDate.get(Calendar.MONTH) == birthDate.get(Calendar.MONTH) &&
                    currentDate.get(Calendar.DAY_OF_MONTH) < birthDate.get(Calendar.DAY_OF_MONTH))
        ) {
            age--
        }
        return age
    }
}