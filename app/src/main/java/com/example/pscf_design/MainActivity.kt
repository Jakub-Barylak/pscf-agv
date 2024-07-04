package com.example.pscf_design

import android.os.Bundle
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.lang.NumberFormatException
import java.nio.charset.StandardCharsets


class MainActivity : AppCompatActivity(), SeekBar.OnSeekBarChangeListener {
    
    private val speedTopic = "speed"
    private val directionTopic = "direction"
    private val agvInfoTopic = "someData"

    private val identifier = "my-mqtt-android-client"
    private var address: String? = null
    private var port: Int? = null
    private var username: String? = null
    private var password: String? = null

    private lateinit var addressTextBox: TextView
    private lateinit var portTextBox: TextView
    private lateinit var usernameTextBox: TextView
    private lateinit var passwordTextBox: TextView

    private var client: Mqtt3AsyncClient? = null

    private lateinit var upButton: Button
    private lateinit var downButton: Button
    private lateinit var leftButton: Button
    private lateinit var rightButton: Button
    private lateinit var connectButton: Button

    private lateinit var agvSpeedSlider: SeekBar
    private var agvSpeed: Int = 0

    private lateinit var agvInfo: TextView

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        coroutineScope.launch {
            while (isActive) {
                if (client != null) {
                    updateMqttClientState()
                }
                delay(1000)
            }
        }

        //connectMqttClient()

        addressTextBox = findViewById(R.id.addressTextbox)
        usernameTextBox = findViewById(R.id.usernameTextBox)
        passwordTextBox = findViewById(R.id.passwordTextBox)
        portTextBox = findViewById(R.id.portTextBox)

        upButton = findViewById(R.id.upButton)
        upButton.setOnClickListener {
//            TODO publish data in the right format
            publish(directionTopic, "up")
        }

        downButton = findViewById(R.id.downButton)
        downButton.setOnClickListener {
//            TODO publish data in the right format
            publish(directionTopic, "down")
        }

        leftButton = findViewById(R.id.leftButton)
        leftButton.setOnClickListener {
//            TODO publish data in the right format
            publish(directionTopic, "left")
        }

        rightButton = findViewById(R.id.rightButton)
        rightButton.setOnClickListener {
//            TODO publish data in the right format
            publish(directionTopic, "right")
        }

        connectButton = findViewById(R.id.connectButton)
        connectButton.setOnClickListener {
            try {
                address = addressTextBox.text.toString()
                port = portTextBox.text.toString().toInt()
                username = usernameTextBox.text.toString()
                password = passwordTextBox.text.toString()
                connectMqttClient()
            } catch (exception: NumberFormatException) {
                Toast.makeText(this, "Please fill out the port", Toast.LENGTH_SHORT).show()
            }


        }

        agvSpeedSlider = findViewById(R.id.seekBar)
        agvSpeedSlider.setOnSeekBarChangeListener(this)

        agvInfo = findViewById(R.id.textView3)

        updateInputs(false)

    }

    override fun onStartTrackingTouch(seek: SeekBar) {}

    override fun onStopTrackingTouch(seek: SeekBar) {
        Toast.makeText(this, "Speed: $agvSpeed", Toast.LENGTH_SHORT).show()
//            TODO publish data in the right format
        publish(speedTopic, agvSpeed.toString())
    }

    override fun onProgressChanged(
        seekBar: SeekBar?, progress: Int,
        fromUser: Boolean
    ) {
        agvSpeed = progress
    }

    override fun onDestroy() {
        super.onDestroy()
        client!!.disconnect()
        coroutineScope.cancel()
    }

    /**
     * Connects, authenticates and subscribes the Mqtt client to a topic.
     */
    private fun connectMqttClient() {
        if (username == null || username == "") {
            Toast.makeText(this, "Please fill out the username", Toast.LENGTH_SHORT).show()
            return
        }
        if (password == null || password == "") {
            Toast.makeText(this, "Please fill out the password", Toast.LENGTH_SHORT).show()
            return
        }
        if (address == null || address == "") {
            Toast.makeText(this, "Please fill in the address", Toast.LENGTH_SHORT).show()
            return
        }
        if (port == null) {
            Toast.makeText(this, "Please fill in the port", Toast.LENGTH_SHORT).show()
            return
        }
        connect()
        authenticate()
        subscribe(agvInfoTopic)
    }

    /**
     * Creates a MQTT client and connects to a MQTT broker.
     * Displays errors as  when address or port is null
     */
    private fun connect() {
        client = MqttClient.builder()
            .useMqttVersion3()
            .identifier(identifier)
            .serverHost(address!!)
            .serverPort(port!!)
            .useSslWithDefaultConfig()
            .buildAsync()
    }

    /**
     * Authenticates with the MQTT broker using username and password.
     */
    private fun authenticate() {
        client!!.connectWith()
            .simpleAuth()
            .username(username!!)
            .password(password!!.toByteArray())
            .applySimpleAuth()
            .send()
    }

    /**
     * Subscribes to a given topic
     */
    private fun subscribe(topic: String) {
        client!!.subscribeWith().topicFilter(topic).callback { publish ->
            val s = StandardCharsets.UTF_8.decode(publish?.payload?.get()).toString()
            agvInfo.text = s
        }.send()
    }

    /**
     * Publishes a message in a given topic.
     */
    private fun publish(topic: String, message: String) {
        client!!.toAsync().publishWith()
            .topic(topic)
            .payload(message.toByteArray())
            .send()
    }

    /**
     * Disables or enables the inputs based on the client state.
     */
    private fun updateMqttClientState() {
        val connected = client!!.state.toString() == "CONNECTED"
        updateInputs(connected)
    }

    private fun updateInputs(connected: Boolean) {
        upButton.isEnabled = connected
        upButton.isClickable = connected
        downButton.isEnabled = connected
        downButton.isClickable = connected
        leftButton.isEnabled = connected
        leftButton.isClickable = connected
        rightButton.isEnabled = connected
        rightButton.isClickable = connected
        agvSpeedSlider.isEnabled = connected
        agvSpeedSlider.isClickable = connected

        connectButton.isEnabled = !connected
        connectButton.isClickable = !connected
    }
}