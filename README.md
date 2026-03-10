# 📦 Pedidos Pro (Pre-Inventarios SA)

Una aplicación nativa de Android diseñada para agilizar el proceso de levantamiento de inventarios y generación de pedidos en piso de venta. Optimizada para velocidad, precisión y uso continuo.

## 🚀 Características Principales

* **Escáner Integrado:** Lectura de códigos de barras usando la cámara del dispositivo para buscar o agregar productos al instante.
* **Sistema Inteligente de Unidades:** * Prevención de errores: Bloquea decimales en "Piezas".
  * Soporte nativo para fracciones en "Cajas" (ej. `1 1/2`).
  * Cálculo exacto para "Kg/g" (ej. `1.2` para 1kg 200g).
* **Calculadora de Proyección en Tiempo Real:** Muestra visualmente (con colores y texto) si el pedido generará un *Faltante*, un *Excedente* o un *Stock Completo* basándose en el stock ideal.
* **Integración Directa con WhatsApp:** Genera un reporte de pedido limpio y formateado y lo envía directamente por WhatsApp.
* **Tolerancia a Fallos (Bóveda):** Historial interno de pedidos. Si WhatsApp falla o se cierra, el texto del pedido queda respaldado con fecha y hora para reenvío.
* **Migración de Datos:** Exportación e importación de la base de datos completa mediante archivos `.csv` (Excel).
* **Respaldo en la Nube:** Configurada con Android Auto Backup para sincronizar la base de datos local con Google Drive automáticamente.
* **Diseño Moderno (Material Design 3):** UI limpia con paleta de colores "Pastel Suave", bordes redondeados y soporte nativo para **Modo Oscuro**.

## 🛠️ Tecnologías y Arquitectura

* **Lenguaje:** Kotlin
* **Interfaz de Usuario:** Jetpack Compose (Material 3)
* **Base de Datos Local:** Room Database (SQLite)
* **Arquitectura:** MVVM (Model-View-ViewModel) + Corrutinas / Flow
* **Hardware:** CameraX (Para el escáner de códigos de barras)


## ⚙️ Instalación y Uso (Para Desarrolladores)

1. Clona este repositorio:
   ```bash
   git clone [https://github.com/ruflezdev/Pre-inventarios-APP](https://github.com/ruflezdev/Pre-inventarios-APP)
2. Abre el proyecto en Android Studio (Koala o superior recomendado).

3. Sincroniza los archivos de Gradle.

4. Conecta tu dispositivo Android o inicia un emulador.

5. Haz clic en Run (Shift + F10).

Nota sobre Permisos: La aplicación requiere permiso de uso de Cámara para la lectura de códigos de barras, el cual se solicita automáticamente en el primer inicio.

👨‍💻 Autor
Desarrollado por José Rubén Ramos Lomeli.
Construido aplicando principios de UX/UI para operabilidad en piso de venta y desarrollo asistido por IA.
