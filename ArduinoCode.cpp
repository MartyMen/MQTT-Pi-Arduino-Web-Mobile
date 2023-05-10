#include <LiquidCrystal.h>

// Create an LCD object. Parameters: (RS, E, D4, D5, D6, D7):
LiquidCrystal lcd(2, 3, 4, 5, 6, 7);
int led = 12;

void setup() {
  // Specify the LCD's number of columns and rows
  lcd.begin(16, 2);
  pinMode(led, OUTPUT);
  Serial.begin(9600); // Initialize Serial communication at 9600 baud rate
}

void loop() {
  if (Serial.available() > 0) {
    String command = Serial.readString();
    command.trim();

    if (command == "TURN_ON_LCD") {
      lcd.clear();
      lcd.setCursor(0, 0);
      lcd.print("LED is ON");
      digitalWrite(led, HIGH);
    }
    if (command == "TURN_OFF_LCD") {
      lcd.clear();
      lcd.setCursor(0, 0);
      lcd.print("LED is OFF");
      digitalWrite(led, LOW);
    }
  }
}
