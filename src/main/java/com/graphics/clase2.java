package com.graphics; // Agrupa las cuatro lecciones dentro del mismo paquete.

import static org.lwjgl.glfw.GLFW.*; // Permite consultar teclas y cambiar el título de la ventana.

/**
 * CLASE 2: CREACIÓN Y MOVIMIENTO DEL AUTO.
 * Responsabilidad: conducción (teclado → física con deltaTime), colisiones, cámara de seguimiento,
 * modelo del auto e indicador del título.
 * Orden de lectura: variables, teclado, movimiento, indicador, colisiones, cámara y dibujo.
 */
public class clase2 extends clase1 {

    // ==================== 1. VARIABLES DEL AUTO ====================
    protected static final float INICIO_X = centro(0); // Punto de partida: avenida del borde oeste (X = -50).
    protected static final float INICIO_Z = centro(MAPA.length - 1); // Punto de partida: avenida del borde sur (Z = 50).
    protected float autoX = INICIO_X; // Posición horizontal inicial: centro de una calle.
    protected float autoZ = INICIO_Z; // Posición inicial sobre la avenida que recorre el fondo de la ciudad.
    protected float angulo = 0; // Orientación en radianes; cero apunta hacia -Z.
    protected float velocidad = 0; // Unidades por segundo; un valor negativo significa reversa.
    protected boolean camaraAerea = false; // false: seguir el auto; true: observar toda la ciudad.
    protected boolean frenando = false; // true mientras el conductor frena: enciende las luces de freno.
    protected static final float RADIO_AUTO = 1.65f; // Radio que contiene al vehículo para las colisiones.

    // ==================== 2. TECLADO Y REINICIO ====================

    /** Atiende acciones que deben ocurrir una sola vez por pulsación. */
    @Override // Sustituye el método de clase1 y conserva sus acciones mediante super.
    protected void tecla(int key) {
        super.tecla(key); // Ejecuta el control ESC definido en clase1.

        if (key == GLFW_KEY_C) { // Comprueba si se presionó la tecla de cámara.
            camaraAerea = !camaraAerea; // Invierte el modo de cámara actual.
        }

        if (key == GLFW_KEY_R) { // Comprueba si el usuario quiere comenzar de nuevo.
            reiniciar(); // Restaura las variables de conducción.
        }
    }

    /** Coloca nuevamente el auto en su punto de partida. */
    protected void reiniciar() {
        autoX = INICIO_X; // Recupera la coordenada X de inicio.
        autoZ = INICIO_Z; // Recupera la coordenada Z de inicio.
        angulo = 0; // Orienta el frente hacia -Z.
        velocidad = 0; // Detiene cualquier movimiento previo.
    }

    // ==================== 3. MOVIMIENTO POR CUADRO ====================

    /** Lee el teclado y entrega las órdenes del conductor a la física; deltaTime son los segundos del cuadro. */
    @Override // Cambia la cámara orbital de clase1 por el control del vehículo.
    protected void actualizar(float deltaTime) {
        float acelerador = 0; // Sin teclas pulsadas no se aplica aceleración del motor.
        if (pulsada(GLFW_KEY_W) || pulsada(GLFW_KEY_UP)) { // Acepta W o flecha arriba para avanzar.
            acelerador += 1; // Solicita aceleración hacia delante.
        }
        if (pulsada(GLFW_KEY_S) || pulsada(GLFW_KEY_DOWN)) { // Acepta S o flecha abajo para retroceder.
            acelerador -= 1; // Primero reduce la velocidad positiva y después entra en reversa.
        }
        float direccion = 0; // Sin dirección presionada el volante permanece recto.
        if (pulsada(GLFW_KEY_A) || pulsada(GLFW_KEY_LEFT)) { // Comprueba el giro hacia la izquierda.
            direccion += 1; // Selecciona el sentido positivo de rotación.
        }
        if (pulsada(GLFW_KEY_D) || pulsada(GLFW_KEY_RIGHT)) { // Comprueba el giro hacia la derecha.
            direccion -= 1; // Selecciona el sentido negativo de rotación.
        }
        mover(acelerador, direccion, pulsada(GLFW_KEY_SPACE), deltaTime); // Aplica la física con las órdenes leídas.
    }

    /**
     * Física de la conducción, separada del teclado para poder probarla sin ventana.
     * Todo cambio se multiplica por deltaTime: el auto recorre lo mismo a 30 o a 144 cuadros por segundo.
     */
    protected void mover(float acelerador, float direccion, boolean freno, float deltaTime) {
        frenando = freno || (acelerador < 0 && velocidad > 0.5f); // Espacio, o S mientras avanza, encienden las luces de freno.
        velocidad += acelerador * 9 * deltaTime; // Integra la aceleración de 9 unidades por segundo cuadrado.
        float resistencia = 0.7f; // Define la pérdida de velocidad normal al rodar.

        if (freno) { // Detecta si el usuario mantiene presionado el freno.
            resistencia = 7; // Aumenta la pérdida de velocidad para detenerse rápidamente.
        }

        float factorFrenado = (float) Math.exp(-resistencia * deltaTime); // Calcula la fracción de velocidad conservada.
        velocidad *= factorFrenado; // Aplica resistencia de forma proporcional al tiempo transcurrido.
        velocidad = Math.max(-6, Math.min(16, velocidad)); // Limita la reversa a -6 y el avance a 16.

        angulo += direccion * velocidad * 0.11f * deltaTime; // Gira según la velocidad; en reversa invierte el giro.
        float frenteX = -(float) Math.sin(angulo); // Obtiene la componente X del frente del vehículo.
        float frenteZ = -(float) Math.cos(angulo); // Obtiene la componente Z; con ángulo cero vale -1.
        float siguienteX = autoX + frenteX * velocidad * deltaTime; // Propone la nueva posición X.
        float siguienteZ = autoZ + frenteZ * velocidad * deltaTime; // Propone la nueva posición Z.

        if (puedeCircular(siguienteX, siguienteZ)) { // Comprueba la posición antes de mover el auto.
            autoX = siguienteX; // Acepta el desplazamiento horizontal.
            autoZ = siguienteZ; // Acepta el desplazamiento en profundidad.
        } else { // La posición propuesta invadiría una manzana o saldría del mapa.
            velocidad = 0; // Detiene el auto conservando su última posición válida.
        }
    }

    // ==================== 4. INDICADOR EN EL TÍTULO ====================

    private String tituloPublicado = ""; // Último texto enviado a la ventana.
    private float esperaTitulo = 0; // Segundos acumulados desde la última publicación.

    /** Publica el indicador como máximo diez veces por segundo y solo si el texto cambió. */
    @Override // Usa el gancho que clase1 llama después de actualizar todas las etapas.
    protected void actualizarIndicador(float deltaTime) {
        esperaTitulo += deltaTime; // Acumula el tiempo desde la última publicación.
        if (esperaTitulo < 0.1f && !tituloPublicado.isEmpty()) { // Aún no pasó una décima de segundo.
            return; // Evita reescribir el título en cada cuadro.
        }
        esperaTitulo = 0; // Reinicia la espera.
        String titulo = textoIndicador(); // Arma el texto con el estado actual.
        if (!titulo.equals(tituloPublicado)) { // Solo se publica si cambió algo.
            glfwSetWindowTitle(ventana, titulo); // Publica el texto en la barra superior de la ventana.
            tituloPublicado = titulo; // Recuerda lo publicado.
        }
    }

    /**
     * Compone el indicador: primero el estado y al final los controles.
     * Si la ventana es angosta y el título se recorta, se pierden los controles y no la información importante.
     */
    protected String textoIndicador() {
        String titulo = "Ciudad | " + kilometrosPorHora() + " km/h"; // Empieza por la velocidad.
        titulo += " | " + SECTORES[sectorEn(autoX, autoZ)]; // Añade el sector por el que circula el auto.
        titulo += estadoExtra(); // Permite a clase3 y clase4 añadir luces y entregas.
        titulo += "   ||   WASD conducir - Espacio freno - C camara - R reiniciar"; // Controles (solo ASCII: el título no depende de la codificación).
        titulo += controlesExtra(); // Permite a clase3 y clase4 añadir sus teclas.
        return titulo; // Entrega el texto completo.
    }

    /** Velocidad del auto en km/h, redondeada; la usan el título y el panel de clase4. */
    protected int kilometrosPorHora() {
        return Math.round(Math.abs(velocidad) * 3.6f); // Convierte unidades por segundo (metros) a km/h.
    }

    /** Deja un espacio para el estado de las próximas lecciones; cada parte empieza con " | ". */
    protected String estadoExtra() {
        return ""; // En clase2 todavía no hay información adicional.
    }

    /** Deja un espacio para las teclas de las próximas lecciones; cada parte empieza con " - ". */
    protected String controlesExtra() {
        return ""; // En clase2 no hay más teclas.
    }

    // ==================== 5. COLISIONES CON LA CIUDAD ====================

    /** Comprueba si el círculo del auto cabe en una posición sin tocar manzanas ni bordes. */
    protected boolean puedeCircular(float x, float z) {
        return libreDeManzanas(x, z, RADIO_AUTO); // El jugador usa el radio de su auto.
    }

    /** Comprueba si un círculo de cierto radio cabe en una posición sin tocar manzanas ni bordes; lo usa también el tráfico. */
    protected static boolean libreDeManzanas(float x, float z, float radio) {
        float limitePermitido = LIMITE - radio; // Reserva espacio para que el vehículo completo quede dentro.

        if (Math.abs(x) > limitePermitido || Math.abs(z) > limitePermitido) { // Detecta salida por cualquier borde.
            return false; // Rechaza la posición exterior.
        }

        for (int fila = 0; fila < MAPA.length; fila++) { // Recorre las filas del mapa.
            for (int columna = 0; columna < MAPA[fila].length; columna++) { // Recorre las celdas de cada fila.
                if (MAPA[fila][columna] == 0) { // Una celda con cero representa calle.
                    continue; // Omite la calle porque no es un obstáculo.
                }

                float centroX = centro(columna); // Convierte la columna al centro X de la manzana.
                float centroZ = centro(fila); // Convierte la fila al centro Z de la manzana.
                float cercaX = Math.max(centroX - 5, Math.min(x, centroX + 5)); // Busca el X más cercano dentro de la acera.
                float cercaZ = Math.max(centroZ - 5, Math.min(z, centroZ + 5)); // Busca el Z más cercano dentro de la acera.
                float distanciaX = x - cercaX; // Calcula la separación horizontal del auto al rectángulo.
                float distanciaZ = z - cercaZ; // Calcula la separación en profundidad al rectángulo.
                float distanciaCuadrada = distanciaX * distanciaX + distanciaZ * distanciaZ; // Aplica Pitágoras sin raíz.

                if (distanciaCuadrada < radio * radio) { // Comprueba si el círculo invade la manzana.
                    return false; // Rechaza el movimiento que produciría una colisión.
                }
            }
        }

        return true; // Acepta la posición porque no se encontró ningún obstáculo.
    }

    // ==================== 6. CÁMARA ====================

    /** Elige entre una vista general y una cámara situada detrás del auto. */
    @Override // Personaliza la cámara creada en clase1.
    protected void configurarCamara() {
        if (camaraAerea) { // Comprueba si está activa la vista general.
            super.configurarCamara(); // Reutiliza la cámara oblicua de la primera lección.
            return; // Evita reemplazarla con la cámara de seguimiento.
        }

        float camaraX = autoX + (float) Math.sin(angulo) * 12; // Coloca la cámara 12 unidades detrás en X.
        float camaraZ = autoZ + (float) Math.cos(angulo) * 12; // Coloca la cámara 12 unidades detrás en Z.
        vector("uOjo", camaraX, 9, camaraZ); // Envía la posición de la cámara, a 9 unidades de altura.
        vector("uObjetivo", autoX, 0.8f, autoZ); // Orienta la cámara hacia la carrocería.
        decimal("uAspecto", (float) ancho / alto); // Mantiene las proporciones al redimensionar la ventana.
    }

    // ==================== 7. DIBUJO DEL AUTO ====================

    /** Dibuja primero la ciudad existente y luego el vehículo. */
    @Override // Amplía el dibujo de clase1 sin copiar su código.
    protected void escena() {
        super.escena(); // Dibuja asfalto, calles, edificios y parques.
        dibujarAuto(); // Añade el modelo del vehículo sobre la ciudad.
    }

    /**
     * Construye un sedán deportivo al estilo Mitsubishi Lancer Evolution X en color fucsia (#FF00FF).
     * Argumentos de pieza(): posición XYZ local (frente hacia -Z), tamaño XYZ y color RGB.
     * Todo cabe dentro del círculo de colisión RADIO_AUTO.
     */
    protected void dibujarAuto() {
        float rojo = 1.0f; // Fucsia #FF00FF: rojo completo.
        float verde = 0.0f; // Fucsia: sin verde.
        float azul = 1.0f; // Fucsia: azul completo.
        float negro = 0.04f; // Negro de parrilla, alerón y faldones.

        // Carrocería: base baja, capó delante, maletero detrás, cabina de vidrio y techo.
        pieza(0, 0.58f, 0, 1.7f, 0.46f, 2.7f, rojo, verde, azul); // Carrocería inferior fucsia.
        pieza(0, 0.84f, -0.78f, 1.62f, 0.08f, 1.05f, rojo, verde, azul); // Capó, más bajo que la cabina.
        pieza(0, 0.86f, 1.05f, 1.62f, 0.1f, 0.55f, rojo, verde, azul); // Tapa del maletero.
        pieza(0, 1.1f, 0.15f, 1.44f, 0.46f, 1.25f, 0.10f, 0.12f, 0.16f); // Cabina de vidrio polarizado.
        pieza(0, 1.35f, 0.18f, 1.34f, 0.06f, 1.0f, rojo, verde, azul); // Techo fucsia.

        // Detalles del Evo X: parrilla trapezoidal, tomas de aire, alerón, faldones, espejos y escape.
        pieza(0, 0.62f, -1.36f, 0.62f, 0.26f, 0.04f, negro, negro, negro); // Parrilla negra central.
        pieza(0, 0.42f, -1.36f, 1.3f, 0.12f, 0.04f, negro, negro, negro); // Toma de aire inferior ancha.
        for (int lado = -1; lado <= 1; lado += 2) { // Lado izquierdo y derecho.
            pieza(lado * 0.3f, 0.885f, -0.75f, 0.26f, 0.02f, 0.32f, negro, negro, negro); // Rejilla en el capó.
            pieza(lado * 0.86f, 0.36f, 0, 0.04f, 0.1f, 1.5f, negro, negro, negro); // Faldón lateral.
            pieza(lado * 0.82f, 1.0f, -0.3f, 0.14f, 0.08f, 0.12f, rojo, verde, azul); // Espejo retrovisor.
            pieza(lado * 0.6f, 1.0f, 1.2f, 0.08f, 0.24f, 0.1f, negro, negro, negro); // Soporte del alerón.
        }
        pieza(0, 1.14f, 1.22f, 1.62f, 0.05f, 0.32f, negro, negro, negro); // Alerón trasero.
        pieza(0.5f, 0.36f, 1.37f, 0.14f, 0.1f, 0.06f, 0.75f, 0.75f, 0.78f); // Escape cromado.

        float[] ladosRuedas = {-0.88f, 0.88f}; // Ubica ruedas a izquierda y derecha del auto.
        float[] ejesRuedas = {-0.85f, 0.85f}; // Ubica las ruedas delanteras y traseras.

        for (float x : ladosRuedas) { // Selecciona uno de los dos lados del vehículo.
            for (float z : ejesRuedas) { // Selecciona el eje delantero o trasero.
                pieza(x, 0.38f, z, 0.24f, 0.58f, 0.6f, 0.055f, 0.065f, 0.08f); // Dibuja un neumático oscuro.
                float exterior = Math.signum(x) * 1.005f; // Cara exterior de la rueda.
                pieza(exterior, 0.38f, z, 0.02f, 0.34f, 0.34f, 0.32f, 0.33f, 0.35f); // Llanta gris oscuro.
            }
        }

        float[] ladosFaros = {-0.55f, 0.55f}; // Define la separación lateral de las luces.
        for (float x : ladosFaros) { // Repite el dibujo para ambos lados.
            if (farosEncendidos()) { // Un faro encendido es una fuente de luz: se ve brillante.
                entero("uEmision", 1); // Dibuja la bombilla sin oscurecerla con la iluminación.
            }
            pieza(x, 0.74f, -1.37f, 0.42f, 0.12f, 0.05f, 1, 0.95f, 0.65f); // Faro delantero rasgado.
            entero("uEmision", 0); // Las demás piezas reciben la luz del entorno.
            float rojoTrasero = 0.85f; // Rojo normal de la luz trasera apagada.
            if (frenando) { // Al frenar la luz trasera se enciende con fuerza.
                entero("uEmision", 1); // La luz de freno brilla por sí misma.
                rojoTrasero = 1; // Usa el rojo más intenso.
            } else if (farosEncendidos()) { // Con los faros encendidos la luz trasera queda tenue.
                entero("uEmision", 1); // La luz de posición también es una fuente de luz.
                rojoTrasero = 0.55f; // Usa un rojo más suave que el de freno.
            }
            pieza(x, 0.74f, 1.37f, 0.38f, 0.12f, 0.06f, rojoTrasero, 0.05f, 0.05f); // Dibuja una luz trasera roja.
            entero("uEmision", 0); // Devuelve la iluminación normal a las siguientes piezas.
        }
    }

    /** Convierte un punto local del auto (X a la derecha, Z hacia atrás) en coordenadas X, Z de la ciudad. */
    protected float[] puntoDelAuto(float x, float z) {
        float coseno = (float) Math.cos(angulo); // Calcula el coseno de la orientación del auto.
        float seno = (float) Math.sin(angulo); // Calcula el seno de la misma orientación.
        return new float[] {autoX + coseno * x + seno * z, autoZ - seno * x + coseno * z}; // Gira y traslada el punto.
    }

    /** Indica si los faros delanteros están encendidos; clase3 añade el interruptor F. */
    protected boolean farosEncendidos() {
        return false; // En clase2 todavía no existen luces que encender.
    }

    /** Transforma una pieza del espacio local del auto al espacio de la ciudad. */
    protected void pieza(float x, float y, float z, float sx, float sy, float sz, float r, float g, float b) {
        piezaEn(autoX, autoZ, angulo, x, y, z, sx, sy, sz, r, g, b); // Usa la posición y orientación del jugador.
    }

    /** Dibuja una pieza local (X a la derecha, Z hacia atrás) de cualquier vehículo ubicado en origen X, Z con un giro. */
    protected void piezaEn(float origenX, float origenZ, float giro, float x, float y, float z, float sx, float sy, float sz, float r, float g, float b) {
        float coseno = (float) Math.cos(giro); // Coseno de la orientación del vehículo.
        float seno = (float) Math.sin(giro); // Seno de la misma orientación.
        float mundoX = origenX + coseno * x + seno * z; // Gira y traslada el punto en X.
        float mundoZ = origenZ - seno * x + coseno * z; // Gira y traslada el punto en Z.
        cajaGirada(mundoX, y, mundoZ, sx, sy, sz, r, g, b, giro); // Dibuja la pieza con la orientación del vehículo.
    }

    /** Punto de entrada para ejecutar únicamente la segunda etapa. */
    public static void main(String[] args) {
        clase2 aplicacion = new clase2(); // Crea la aplicación con ciudad y vehículo.
        aplicacion.run(); // Inicia la ventana y el ciclo de dibujo heredados.
    }
}
