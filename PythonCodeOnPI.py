import paho.mqtt.client as mqtt
import serial
import time
from sense_hat import SenseHat
import random

mqtt_server = "Your Server"
mqtt_topic = "lcd/control"
mqtt_status_topic = "lcd/status"
mqtt_request_topic = "lcd/request_state"
mqtt_temperature_topic = "sensehat/temperature"
mqtt_humidity_topic = "sensehat/humidity"
mqtt_gyro_topic = "mobile/gyro"

arduino_port = "/dev/ttyACM0"
baud_rate = 9600

# establish connection
arduino = serial.Serial(arduino_port, baud_rate)
time.sleep(3)

s = SenseHat()
s.clear()

led_status = "LED_OFF"

def on_connect(client, userdata, flags, rc):
    print("Connected with result code: " + str(rc))
    client.subscribe(mqtt_topic)
    client.subscribe(mqtt_request_topic)
    client.subscribe(mqtt_gyro_topic)

def set_sh_color(color):
    for x in range(8):
        for y in range(8):
            s.set_pixel(x, y, color)

def on_message(client, userdata, msg):
    global led_status
    print(msg.topic + " " + str(msg.payload))

    command = msg.payload.decode("utf-8")

    if command == "TURN_ON_LCD":
        arduino.write(msg.payload)
        set_sh_color((0, 255, 0))
        led_status = "LED_ON"
    elif command == "TURN_OFF_LCD":
        arduino.write(msg.payload)
        set_sh_color((0, 0, 0))
        led_status = "LED_OFF"
    elif command == "REQUEST_STATE":
        pass  # Do nothing, just update the status below
    elif msg.topic == mqtt_gyro_topic:
        r = random.randint(0, 255)
        g = random.randint(0, 255)
        b = random.randint(0, 255)
        set_sh_color((r, g, b))
    else:
        print("invalid command received")
        return

    client.publish(mqtt_status_topic, led_status)

def publish_sensor_data(client):
    temperature = round(s.get_temperature(), 2)
    humidity = round(s.get_humidity(), 2)
    client.publish(mqtt_temperature_topic, temperature)
    client.publish(mqtt_humidity_topic, humidity)

client = mqtt.Client()
client.on_connect = on_connect
client.on_message = on_message
client.connect(mqtt_server, 1883, 60)

try:
    while True:
        client.loop()
        publish_sensor_data(client)
        time.sleep(5)
except KeyboardInterrupt:
    print("Exiting...")
finally:
    arduino.close()
    s.clear()
