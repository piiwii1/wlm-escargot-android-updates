package ch.piiwii.remote2.ui

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import ch.piiwii.remote2.BuildConfig
import ch.piiwii.remote2.device.DeviceType
import ch.piiwii.remote2.device.SharedPreferencesDeviceRepository
import ch.piiwii.remote2.protocol.Protocol
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()
    private lateinit var shell: MainShellView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = UiKit.BG
        window.navigationBarColor = UiKit.BG

        shell = MainShellView(this).also {
            it.onSectionClick(vm::chooseSection)
            it.onDeviceClick(::showDeviceChooser)
            it.onSettingsClick(::showSettings)
            it.onCommandClick(vm::sendCommand)
        }
        setContentView(shell)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(vm.section, vm.selected, vm.connection, vm.touchpadSettings) { section, device, connection, touchpad ->
                    RenderState(section, device, connection, touchpad)
                }.collect { state ->
                    shell.render(state.section, state.device, state.connection, state.touchpad)
                }
            }
        }
    }

    private fun showDeviceChooser() {
        val devices = vm.devices.value
        val labels = devices.map { "${it.name}\n${it.type.label} · ${it.host}:${it.port}" }.toMutableList()
        labels.add("＋ Ajouter un appareil")
        AlertDialog.Builder(this)
            .setTitle("Appareils")
            .setItems(labels.toTypedArray()) { _, which ->
                if (which == devices.size) showAddDevice() else vm.selectDevice(devices[which].id)
            }
            .setNegativeButton("Fermer", null)
            .show()
    }

    private fun showAddDevice() {
        val wrap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val p = UiKit.dp(this@MainActivity, 20)
            setPadding(p, UiKit.dp(this@MainActivity, 6), p, 0)
        }
        val type = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, DeviceType.entries.map { it.label })
        }
        val name = field("Nom (ex. PC Bureau)")
        val host = field("Adresse IP (ex. 192.168.1.50)")
        val port = field("Port").apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(SharedPreferencesDeviceRepository.DEFAULT_PORT.toString())
        }
        wrap.addView(type); wrap.addView(name); wrap.addView(host); wrap.addView(port)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Ajouter un appareil")
            .setView(wrap)
            .setPositiveButton("Ajouter", null)
            .setNegativeButton("Annuler", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val deviceName = name.text.toString().trim()
                val deviceHost = host.text.toString().trim()
                val devicePort = port.text.toString().toIntOrNull()
                if (deviceName.isEmpty()) { name.error = "Nom requis"; return@setOnClickListener }
                if (!isValidHost(deviceHost)) { host.error = "Adresse IP ou nom d’hôte invalide"; return@setOnClickListener }
                if (devicePort == null || devicePort !in 1..65535) { port.error = "Port invalide"; return@setOnClickListener }
                vm.addDevice(deviceName, DeviceType.entries[type.selectedItemPosition], deviceHost, devicePort)
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun field(hint: String) = EditText(this).apply {
        this.hint = hint
        setSingleLine(true)
        setPadding(0, UiKit.dp(this@MainActivity, 12), 0, UiKit.dp(this@MainActivity, 12))
    }

    private fun isValidHost(value: String): Boolean {
        if (value.isBlank() || value.length > 253 || value.contains(' ')) return false
        return value.all { it.isLetterOrDigit() || it == '.' || it == '-' || it == ':' }
    }

    private fun showSettings() {
        val message = "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})\n" +
            "Package ${BuildConfig.APPLICATION_ID}\n" +
            "Protocole ${Protocol.VERSION}\n\n" +
            "Lot 4 — le pavé tactile PC contrôle réellement la souris de l’Agent Windows. " +
            "Les réglages avancés du pavé tactile restent réservés au Lot 9 et le clavier distant au Lot 5."
        AlertDialog.Builder(this)
            .setTitle("PiiWii Remote")
            .setMessage(message)
            .setPositiveButton("Fermer", null)
            .show()
    }

    private data class RenderState(
        val section: RemoteSection,
        val device: ch.piiwii.remote2.device.RemoteDevice?,
        val connection: ConnectionState,
        val touchpad: TouchpadSettings
    )
}
