const turnOnButton = document.getElementById("turnOn");
const turnOffButton = document.getElementById("turnOff");
const statusElement = document.getElementById("status");

let mqtt_server = "your server";
let client = new Paho.MQTT.Client(mqtt_server, 9001, "web_client");

client.onConnectionLost = onConnectionLost;
client.onMessageArrived = onMessageArrived;

client.connect({onSuccess: onConnect, onFailure: onFailure});

function onConnect() {
    statusElement.textContent = "Connected";
    client.subscribe("lcd/status");
    client.subscribe("mobile/gyro");
    client.subscribe("sensehat/temperature");
    client.subscribe("sensehat/humidity");
    requestLEDState();
}
function requestLEDState() {
    let message = new Paho.MQTT.Message("REQUEST_STATE");
    message.destinationName = "lcd/request_state";
    client.send(message);
}

function onFailure(invocationContext, errorCode, errorMessage) {
    statusElement.textContent = `Failed to connect: ${errorCode}, ${errorMessage}`;
}

function onConnectionLost(responseObject) {
    if (responseObject.errorCode !== 0) {
        statusElement.textContent = "Disconnected";
    }
}

function onMessageArrived(message) {
    if (message.destinationName === "lcd/status") {
        if (message.payloadString === "LED_ON") {
            statusElement.textContent = "Connected - LED ON";
        } else if (message.payloadString === "LED_OFF") {
            statusElement.textContent = "Connected - LED OFF";
        }
    } else if (message.destinationName === "mobile/gyro") {
        if (message.payloadString === "GYRO_MOVEMENT") {
            statusElement.textContent += " - Phone is moving";
        }
    } else if (message.destinationName === "sensehat/temperature") {
        const temperatureElement = document.getElementById("temperature");
        temperatureElement.textContent = `Temperature: ${message.payloadString}°C`;
    } else if (message.destinationName === "sensehat/humidity") {
        const humidityElement = document.getElementById("humidity");
        humidityElement.textContent = `Humidity: ${message.payloadString}%`;
    }
}



turnOnButton.addEventListener("click", () => {
    let message = new Paho.MQTT.Message("TURN_ON_LCD");
    message.destinationName = "lcd/control";
    client.send(message);
});

turnOffButton.addEventListener("click", () => {
    let message = new Paho.MQTT.Message("TURN_OFF_LCD");
    message.destinationName = "lcd/control";
    client.send(message);
});
