# Guardião — Sistema IoT de Alerta para Assistência

> Um sistema integrado de IoT e desenvolvimento mobile para auxiliar cuidadores e pessoas em situação de vulnerabilidade com notificações de emergência em tempo real.

---

O **Guardião** é uma solução de prototipagem IoT combinada a um aplicativo Android nativo. Seu objetivo principal é garantir o envio rápido de alertas em situações de assistência ou emergência. 

Ao pressionar o botão físico de acionamento do dispositivo, uma requisição é disparada via Wi-Fi para salvar o evento na nuvem, emitindo instantaneamente uma notificação push no smartphone do cuidador cadastrado.

---

## Arquitetura

1. **Envio dos Alertas (Hardware / IoT):**
   - Placa de microcontrolador **ESP32** com botão físico acoplado.
   - Ao ser acionado, envia uma requisição `HTTP POST` para a API REST do **Firebase Firestore**.
   - Payload em formato JSON contendo identificador do dispositivo e timestamp.

2. **Recebimento e Visualização (Mobile):**
   - Aplicativo Android nativo que utiliza o SDK do **Firebase Firestore** para escutar mudanças no banco de dados em tempo real.
   - Notificação no sistema assim que um novo alerta é identificado.
   - Tela para listagem e consulta do histórico de acionamentos.

---

## Tecnologias Utilizadas

### Botão
- **ESP32** (Microcontrolador Wi-Fi)
- **C++** (Código-fonte gravado na ESP32)
- **Arduino IDE** (Ambiente de desenvolvimento)

### App
- **Kotlin** (Linguagem principal do App)
- **Android Studio** (IDE para desenvolvimento Android)
- **Firebase Firestore** (Banco de dados NoSQL em nuvem)
