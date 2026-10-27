#include <WiFi.h>
#include <WiFiClientSecure.h>
#include <HTTPClient.h>
#include <time.h>

const char* WIFI_SSID     = "";
const char* WIFI_PASSWORD = "";

const char* API_KEY    = "";
const char* PROJECT_ID = "";

const char* DISPOSITIVO  = "Familiar X";
const char* ALERT_STATUS = "Emergência";
const double LATITUDE    = -23.522220;
const double LONGITUDE   = -46.7579067;

const int buttonPin = 26;

const unsigned long DEBOUNCE_MS = 50;
const unsigned long COOLDOWN_MS = 3000;

int lastReading = HIGH;
int stableState = HIGH;
unsigned long lastChange = 0;
unsigned long lastSent = 0;
bool hasSent = false;

bool timeOk() {
  return time(nullptr) > 1700000000;
}

void aoDesconectar(WiFiEvent_t event, WiFiEventInfo_t info) {
  Serial.print("Wi-Fi desconectado. Motivo (codigo): ");
  Serial.println(info.wifi_sta_disconnected.reason);
}

void listarRedes() {
  Serial.println("Procurando redes...");
  int n = WiFi.scanNetworks(false, true); 
  Serial.printf("%d redes encontradas\n", n);
  for (int i = 0; i < n; i++) {
    Serial.printf("%2d | %-32s | sinal %d dBm | canal %d | seguranca %d\n",
                  i + 1, WiFi.SSID(i).c_str(), WiFi.RSSI(i),
                  WiFi.channel(i), (int)WiFi.encryptionType(i));
  }
  WiFi.scanDelete();
}

void connectWiFi() {
  if (WiFi.status() == WL_CONNECTED) return;

  WiFi.mode(WIFI_STA);
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  Serial.print("Conectando ao Wi-Fi");
  

  unsigned long start = millis();
  while (WiFi.status() != WL_CONNECTED && millis() - start < 20000) {
    delay(500);
    Serial.print(".");
  }

  if (WiFi.status() == WL_CONNECTED) {
    Serial.print("\nWi-Fi conectado. IP: ");
    Serial.println(WiFi.localIP());
    
  } else {
    Serial.printf("\nFalha ao conectar no Wi-Fi. status final: %d\n", WiFi.status());
  }
}

void syncTime() {
  configTime(0, 0, "pool.ntp.org", "time.google.com");
  unsigned long start = millis();
  while (!timeOk() && millis() - start < 10000) {
    delay(200);
  }
  Serial.println(timeOk() ? "Horario sincronizado (NTP)." : "Nao foi possivel sincronizar o horario.");
}

bool enviarAlerta() {
  if (WiFi.status() != WL_CONNECTED) {
    connectWiFi();
    if (WiFi.status() != WL_CONNECTED) return false;
  }
  if (!timeOk()) syncTime();

  char body[512];
  if (timeOk()) {
    char ts[25];
    time_t now = time(nullptr);
    struct tm t;
    gmtime_r(&now, &t);
    strftime(ts, sizeof(ts), "%Y-%m-%dT%H:%M:%SZ", &t);

    snprintf(body, sizeof(body),
      "{\"fields\":{"
      "\"dispositivo\":{\"stringValue\":\"%s\"},"
      "\"status\":{\"stringValue\":\"%s\"},"
      "\"latitude\":{\"doubleValue\":%.6f},"
      "\"longitude\":{\"doubleValue\":%.6f},"
      "\"horario\":{\"timestampValue\":\"%s\"}"
      "}}",
      DISPOSITIVO, ALERT_STATUS, LATITUDE, LONGITUDE, ts);
  } else {
    snprintf(body, sizeof(body),
      "{\"fields\":{"
      "\"dispositivo\":{\"stringValue\":\"%s\"},"
      "\"status\":{\"stringValue\":\"%s\"},"
      "\"latitude\":{\"doubleValue\":%.6f},"
      "\"longitude\":{\"doubleValue\":%.6f}"
      "}}",
      DISPOSITIVO, ALERT_STATUS, LATITUDE, LONGITUDE);
  }

  String url = String("https://firestore.googleapis.com/v1/projects/") + PROJECT_ID +
               "/databases/(default)/documents/alertas?key=" + API_KEY;

  WiFiClientSecure client;
  client.setInsecure(); 

  HTTPClient http;
  http.begin(client, url);
  http.addHeader("Content-Type", "application/json");

  int code = http.POST((uint8_t*)body, strlen(body));
  Serial.print("Resposta HTTP: ");
  Serial.println(code);

  if (code != 200) {
    Serial.println(http.getString());
  }
  http.end();

  return code == 200;
}

void setup() {
  Serial.begin(115200);
  delay(1000);
  pinMode(buttonPin, INPUT_PULLUP);

  WiFi.mode(WIFI_STA);
  WiFi.disconnect();
  delay(100);
  WiFi.onEvent(aoDesconectar, ARDUINO_EVENT_WIFI_STA_DISCONNECTED);

  listarRedes();
  connectWiFi();
  if (WiFi.status() == WL_CONNECTED) syncTime();
}

void loop() {
  int reading = digitalRead(buttonPin);

  if (reading != lastReading) {
    lastChange = millis();
    lastReading = reading;
  }

  if (millis() - lastChange > DEBOUNCE_MS && reading != stableState) {
    stableState = reading;

    if (stableState == LOW) {
      Serial.println(0);

      bool cooldownOk = !hasSent || (millis() - lastSent >= COOLDOWN_MS);
      if (cooldownOk) {
        if (enviarAlerta()) {
          Serial.println("Alerta enviado ao Firestore.");
        } else {
          Serial.println("Falha ao enviar o alerta.");
        }
        hasSent = true;
        lastSent = millis();
      } else {
        Serial.println("Aguardando intervalo entre alertas.");
      }
    }
  }

  delay(10);
}