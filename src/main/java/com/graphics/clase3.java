package com.graphics; // Mantiene esta lección junto a las clases anteriores.

import static org.lwjgl.glfw.GLFW.*; // Incluye las constantes de las teclas N y F.

/**
 * CLASE 3: ILUMINACIÓN DE LA CIUDAD.
 * Responsabilidad: iluminación. Día y noche, farolas, faros del auto y el shader de luz.
 * Orden de lectura: estado, controles, cambio día/noche, uniforms, farolas y shader.
 */
public class clase3 extends clase2 {

    // ==================== 1. ESTADO Y POSICIONES DE LAS LUCES ====================
    protected boolean noche = true; // Ambiente elegido con N: true pide noche, false pide día.
    protected boolean faros = true; // Inicia los focos del auto encendidos.
    protected float factorNoche = 1; // 0 día, 1 noche; cambia poco a poco.
    protected static final float DURACION_TRANSICION = 1.2f; // Segundos que tarda el paso de día a noche.
    protected static final float ALTURA_BOMBILLA = 4.6f; // Altura desde la que cada farola emite su luz.
    protected static final int PLAZA = MAPA.length / 2; // Fila y columna del parque central (plaza con fuente).

    /** Cada fila: X, Y, Z de la bombilla (lo que usa el shader) y X, Z del poste sobre la acera. */
    protected static final float[][] LUCES = crearFarolas();

    /**
     * Coloca una farola junto a las intersecciones alternadas como un tablero de ajedrez.
     * Así cada calle tiene una farola cerca y todos los sectores quedan iluminados.
     * El poste va en la esquina de acera de la manzana vecina y su brazo lleva la bombilla sobre la calle.
     * Además, la plaza central lleva una farola en cada una de sus cuatro esquinas.
     */
    private static float[][] crearFarolas() {
        java.util.List<float[]> farolas = new java.util.ArrayList<>(); // Acumula las posiciones encontradas.
        int ultima = MAPA.length - 1; // Índice de la última fila y columna del mapa.
        for (int fila = 0; fila <= ultima; fila += 2) { // Recorre las filas de calle (pares).
            for (int columna = 0; columna <= ultima; columna += 2) { // Recorre las columnas de calle (pares).
                if ((fila / 2 + columna / 2) % 2 != 0) { // Salta una intersección de cada dos.
                    continue; // Pasa a la siguiente intersección.
                }
                farolas.add(farola(esquinaAcera(fila, columna))); // Farola en la esquina de acera que toca esta intersección.
            }
        }
        for (int haciaZ = -1; haciaZ <= 1; haciaZ += 2) { // Esquinas norte y sur de la plaza central.
            for (int haciaX = -1; haciaX <= 1; haciaX += 2) { // Esquinas oeste y este.
                float posteX = centro(PLAZA) + haciaX * 4.3f; // Esquina de la acera en X.
                float posteZ = centro(PLAZA) + haciaZ * 4.3f; // Esquina de la acera en Z.
                boolean repetida = false; // El tablero de ajedrez ya pudo poner farola en esta esquina.
                for (float[] existente : farolas) { // Compara con las farolas ya creadas.
                    if (Math.abs(existente[3] - posteX) < 0.01f && Math.abs(existente[4] - posteZ) < 0.01f) { // Mismo poste.
                        repetida = true; // No se duplica.
                    }
                }
                if (!repetida) { // Esquina libre.
                    farolas.add(farola(new float[] {posteX, posteZ, haciaX, haciaZ})); // Agrega la farola de la plaza.
                }
            }
        }
        return farolas.toArray(new float[0][]); // Convierte la lista en la matriz usada por el shader.
    }

    /** Arma una farola a partir de una esquina (poste X, Z y sentido X, Z hacia el cruce): bombilla XYZ y poste XZ. */
    private static float[] farola(float[] esquina) {
        float posteX = esquina[0]; // Ubica el poste en la esquina de la acera.
        float posteZ = esquina[1]; // Ubica el poste en la esquina de la acera.
        float bombillaX = posteX + esquina[2] * 1.0f; // El brazo adelanta la bombilla sobre el borde de la calle.
        float bombillaZ = posteZ + esquina[3] * 1.0f; // El brazo adelanta la bombilla sobre el borde de la calle.
        return new float[] {bombillaX, ALTURA_BOMBILLA, bombillaZ, posteX, posteZ}; // Bombilla y poste.
    }

    /**
     * Devuelve la esquina de acera junto a una intersección: poste X, poste Z y sentido X, Z hacia el cruce (-1 o 1).
     * Usa la manzana de abajo a la derecha, o la de arriba / izquierda en los bordes sur y este.
     * Farolas y semáforos (clase4) la comparten, así cada poste queda en una esquina distinta.
     */
    protected static float[] esquinaAcera(int fila, int columna) {
        int ultima = MAPA.length - 1; // Índice de la última fila y columna del mapa.
        int filaManzana = fila < ultima ? fila + 1 : fila - 1; // Elige la manzana de abajo, o la de arriba en el borde sur.
        int columnaManzana = columna < ultima ? columna + 1 : columna - 1; // Elige la manzana derecha, o la izquierda en el borde este.
        float haciaX = columna - columnaManzana; // Vale -1 o 1: sentido X hacia la intersección.
        float haciaZ = fila - filaManzana; // Vale -1 o 1: sentido Z hacia la intersección.
        float posteX = centro(columnaManzana) + haciaX * 4.3f; // Esquina de la acera en X.
        float posteZ = centro(filaManzana) + haciaZ * 4.3f; // Esquina de la acera en Z.
        return new float[] {posteX, posteZ, haciaX, haciaZ}; // Entrega posición y sentido.
    }

    // ==================== 2. CONTROLES E INDICADORES ====================

    /** Añade los interruptores de iluminación a los controles existentes. */
    @Override // Amplía el método de teclado de clase2.
    protected void tecla(int key) {
        super.tecla(key); // Conserva ESC, C y R.
        if (key == GLFW_KEY_N) { // Comprueba la tecla de cambio de ambiente.
            noche = !noche; // Cambia entre día y noche.
        }
        if (key == GLFW_KEY_F) { // Comprueba el interruptor de los focos del auto.
            faros = !faros; // Enciende los faros si estaban apagados y viceversa.
        }
    }

    /** Prepara el estado de iluminación que clase2 incorpora al título de la ventana. */
    @Override // Añade información al espacio reservado para indicadores.
    protected String estadoExtra() {
        String ambiente = "Dia"; // Usa día como texto inicial.
        if (noche) { // Comprueba si está seleccionado el ambiente nocturno.
            ambiente = "Noche"; // Reemplaza el texto por el estado real.
        }
        String estadoFaros = "OFF"; // Usa apagado como texto inicial de los focos.
        if (faros) { // Comprueba si las luces del auto están activas.
            estadoFaros = "ON"; // Indica que los faros están encendidos.
        }
        return " | " + ambiente + " | Faros " + estadoFaros; // Devuelve ambos indicadores.
    }

    /** Añade las teclas de iluminación a la parte de controles del título. */
    @Override // Amplía el espacio reservado por clase2.
    protected String controlesExtra() {
        return " - N dia/noche - F faros"; // Teclas de ambiente y faros.
    }

    /** Indica a clase2 que dibuje los faros como bombillas encendidas. */
    @Override // Sustituye el valor fijo false de clase2.
    protected boolean farosEncendidos() {
        return faros; // Los faros brillan cuando el interruptor F está activo.
    }

    // ==================== 3. TRANSICIÓN SUAVE ENTRE DÍA Y NOCHE ====================

    /** Conduce el auto como en clase2 y además avanza la transición de ambiente. */
    @Override // Amplía la actualización de clase2.
    protected void actualizar(float deltaTime) {
        super.actualizar(deltaTime); // Procesa conducción, colisiones y título.
        avanzarTransicion(deltaTime); // Acerca la luz al ambiente elegido con N.
    }

    /** Mueve factorNoche hacia 1 (noche) o 0 (día) a velocidad constante. */
    protected void avanzarTransicion(float deltaTime) {
        float objetivo = noche ? 1 : 0; // Valor al que debe llegar la mezcla.
        float paso = deltaTime / DURACION_TRANSICION; // Fracción del cambio que corresponde a este cuadro.
        if (factorNoche < objetivo) { // La escena se está oscureciendo.
            factorNoche = Math.min(objetivo, factorNoche + paso); // Avanza sin pasarse del objetivo.
        } else { // La escena se está aclarando o ya llegó.
            factorNoche = Math.max(objetivo, factorNoche - paso); // Retrocede sin pasarse del objetivo.
        }
    }

    /** Indica si ya es lo bastante de noche como para encender bombillas y ventanas. */
    protected boolean lucesEncendidas() {
        return factorNoche > 0.5f; // Las luces se encienden a mitad de la transición.
    }

    /** Mezcla el celeste del día con el azul oscuro de la noche según la transición. */
    @Override // Sustituye el azul fijo de clase1.
    protected float[] colorCielo() {
        float[] dia = {0.53f, 0.72f, 0.90f}; // Celeste diurno.
        float[] oscuro = {0.03f, 0.05f, 0.11f}; // Azul casi negro nocturno.
        float[] cielo = new float[3]; // Guarda la mezcla de ambos colores.
        for (int canal = 0; canal < 3; canal++) { // Mezcla rojo, verde y azul por separado.
            cielo[canal] = dia[canal] + (oscuro[canal] - dia[canal]) * factorNoche; // Mezcla día y noche.
        }
        return cielo; // Devuelve el color del fondo para este cuadro.
    }

    // ==================== 4. DATOS QUE RECIBE EL SHADER ====================

    /** Actualiza los uniforms de iluminación antes de dibujar la ciudad. */
    @Override // Sustituye el método vacío de clase1.
    protected void prepararLuces() {
        decimal("uFactorNoche", factorNoche); // Comunica la mezcla actual entre día y noche.
        entero("uFaros", 0); // Inicialmente desactiva los conos de luz del auto.
        if (faros) { // Revisa el interruptor de los faros.
            entero("uFaros", 1); // Activa el cálculo de los dos focos en el shader.
        }
        entero("uEmision", 0); // Hace que los objetos normales reciban iluminación.

        for (int indice = 0; indice < LUCES.length; indice++) { // Recorre todas las bombillas.
            float x = LUCES[indice][0]; // Lee la coordenada horizontal de la bombilla.
            float y = LUCES[indice][1]; // Lee su altura sobre el suelo.
            float z = LUCES[indice][2]; // Lee su coordenada en profundidad.
            vector("uLuces[" + indice + "]", x, y, z); // Envía esa posición al arreglo GLSL.
        }

        float frenteX = -(float) Math.sin(angulo); // Calcula hacia dónde apunta el auto en X.
        float frenteZ = -(float) Math.cos(angulo); // Calcula hacia dónde apunta el auto en Z.
        vector("uAuto", autoX, 0.7f, autoZ); // Envía el centro del vehículo a la altura de sus focos.
        vector("uFrente", frenteX, 0, frenteZ); // Envía la dirección frontal del vehículo.
    }

    // ==================== 5. MODELOS DE LAS FAROLAS ====================

    /** Dibuja cada farola en la misma posición usada para calcular su luz. */
    @Override // Añade geometría a la ciudad y al auto heredados.
    protected void escena() {
        super.escena(); // Dibuja la ciudad y el vehículo de las primeras dos lecciones.
        for (float[] luz : LUCES) { // Selecciona una farola a la vez.
            dibujarFarola(luz); // Construye poste, brazo, carcasa y bombilla.
        }
    }

    /** Construye una farola con cajas: poste, brazo en forma de L y bombilla. */
    private void dibujarFarola(float[] luz) {
        float bombillaX = luz[0]; // Posición X de la bombilla.
        float bombillaZ = luz[2]; // Posición Z de la bombilla.
        float posteX = luz[3]; // Posición X del poste en la acera.
        float posteZ = luz[4]; // Posición Z del poste en la acera.
        caja(posteX, 2.7f, posteZ, 0.16f, 4.5f, 0.16f, 0.22f, 0.25f, 0.29f); // Poste.
        caja((posteX + bombillaX) / 2, 4.95f, posteZ, 1.1f, 0.1f, 0.1f, 0.22f, 0.25f, 0.29f); // Brazo en X.
        caja(bombillaX, 4.95f, (posteZ + bombillaZ) / 2, 0.1f, 0.1f, 1.1f, 0.22f, 0.25f, 0.29f); // Brazo en Z.
        float brillo = 0.45f; // Bombilla apagada de día.
        if (lucesEncendidas()) { // De noche la bombilla se enciende.
            entero("uEmision", 1); // La bombilla brilla por sí misma.
            brillo = 1; // Color completo.
        }
        caja(bombillaX, 4.75f, bombillaZ, 0.5f, 0.2f, 0.5f, brillo, brillo * 0.83f, brillo * 0.5f); // Bombilla.
        entero("uEmision", 0); // Lo siguiente recibe luz normal.
    }

    // ==================== 6. CÁLCULO DE LUZ EN LA GPU ====================

    /** Shader de fragmentos: calcula el color iluminado de cada punto. */
    @Override // Reemplaza el shader de color plano de clase1.
    protected String fragmentShader() {
        return """
            #version 330 core
            // Selecciona la versión GLSL correspondiente a OpenGL 3.3.
            #define NUM_LUCES CANTIDAD_FAROLAS
            // Java reemplaza CANTIDAD_FAROLAS por LUCES.length antes de compilar.
            in vec3 vMundo; // Posición del punto en la ciudad.
            in vec3 vNormal; // Hacia dónde mira la cara.
            uniform vec3 uColor; // Color de la caja.
            uniform vec3 uLuces[NUM_LUCES]; // Posiciones de las farolas.
            uniform vec3 uAuto; // Posición del auto a la altura de los faros.
            uniform vec3 uFrente; // Hacia dónde mira el auto.
            uniform float uFactorNoche; // 0 de día, 1 de noche.
            uniform int uFaros; // 1 si los faros están encendidos.
            uniform int uEmision; // 1 si la caja brilla sola (bombillas).
            uniform int uMapa; // 1 al dibujar el minimapa.
            out vec4 color; // Color final.

            void main() {
                if (uEmision == 1 || uMapa == 1) { // Bombillas y minimapa no se iluminan.
                    color = vec4(uColor, 1.0); // Usan su color tal cual.
                    return;
                }

                float noche = uFactorNoche; // Nombre corto.
                vec3 normal = normalize(vNormal); // Normal de largo 1.

                // Luz ambiente: un mínimo de luz para que nada quede negro.
                vec3 luz = mix(vec3(0.42, 0.45, 0.50), vec3(0.14, 0.16, 0.24), noche); // Clara de día, azul de noche.

                // Sol de día y luna de noche: luz difusa (cuanto más mira la cara a la luz, más clara).
                vec3 direccionSol = normalize(vec3(0.4, 1.0, 0.3)); // De dónde viene la luz.
                vec3 colorSol = mix(vec3(0.72, 0.68, 0.60), vec3(0.10, 0.13, 0.22), noche); // Sol cálido, luna azul.
                luz += colorSol * max(dot(normal, direccionSol), 0.0); // Luz difusa.

                // Farolas: cada una ilumina menos cuanto más lejos está.
                if (noche > 0.0) { // Solo de noche.
                    vec3 colorFarola = vec3(1.0, 0.72, 0.38); // Luz naranja.
                    for (int i = 0; i < NUM_LUCES; i++) { // Suma cada farola.
                        vec3 haciaLuz = uLuces[i] - vMundo; // Del punto a la farola.
                        float distancia = length(haciaLuz); // Distancia a la farola.
                        float difusa = max(dot(normal, haciaLuz / distancia), 0.0); // Cuánto mira la cara a la farola.
                        float atenuacion = 1.0 / (1.0 + 0.10 * distancia + 0.035 * distancia * distancia); // Baja con la distancia.
                        float alcance = 1.0 - smoothstep(14.0, 22.0, distancia); // Lejos ya no ilumina.
                        luz += colorFarola * difusa * 4.0 * atenuacion * alcance * noche; // Suma esta farola.
                    }
                }

                // Faros del auto: dos conos de luz hacia adelante con alcance limitado.
                if (uFaros == 1) { // Solo si están encendidos.
                    vec3 lateral = vec3(-uFrente.z, 0.0, uFrente.x); // Lado derecho del auto.
                    vec3 eje = normalize(uFrente + vec3(0.0, -0.10, 0.0)); // Apunta un poco hacia el suelo.
                    vec3 colorFaro = vec3(1.0, 0.94, 0.72); // Luz blanca cálida.
                    for (int i = 0; i < 2; i++) { // Faro izquierdo y derecho.
                        float separacion = i == 0 ? -0.55 : 0.55; // Lado del faro.
                        vec3 origen = uAuto + uFrente * 1.32 + lateral * separacion; // Posición del faro.
                        vec3 haciaPunto = vMundo - origen; // Del faro al punto.
                        float distancia = length(haciaPunto); // Distancia al faro.
                        vec3 direccion = haciaPunto / max(distancia, 0.001); // Dirección del rayo.
                        float cono = smoothstep(0.85, 0.97, dot(direccion, eje)); // 1 dentro del cono, 0 fuera.
                        float difusa = max(dot(normal, -direccion), 0.0); // Cuánto mira la cara al faro.
                        float alcance = 1.0 - smoothstep(20.0, 28.0, distancia); // Se apaga entre 20 y 28.
                        luz += colorFaro * difusa * cono * alcance * 8.0 / (1.0 + 0.04 * distancia * distancia); // Suma este faro.
                    }
                }

                color = vec4(uColor * luz, 1.0); // Color de la caja por la luz que recibe.
            }
            """.replace("CANTIDAD_FAROLAS", String.valueOf(LUCES.length)); // Fija el tamaño del arreglo de farolas.
    }

    /** Punto de entrada de la tercera lección. */
    public static void main(String[] args) {
        clase3 aplicacion = new clase3(); // Crea la etapa con iluminación.
        aplicacion.run(); // Inicia el ciclo de vida heredado de clase1.
    }
}
