package com.graphics; // Mantiene esta lección junto a las clases anteriores.

import static org.lwjgl.glfw.GLFW.*; // Incluye las constantes de las teclas N y F.
import static org.lwjgl.opengl.GL33.*; // Permite activar la mezcla aditiva de los halos de luz.

/**
 * CLASE 3: ILUMINACIÓN DE LA CIUDAD.
 * Conserva ciudad y conducción; añade sol, luna, farolas y focos del vehículo.
 * Orden de lectura: estado, controles, transición día/noche, uniforms, farolas, halos y shader.
 * El shader calcula iluminación local: este ejemplo todavía no proyecta sombras.
 */
public class clase3 extends clase2 {

    // ==================== 1. ESTADO Y POSICIONES DE LAS LUCES ====================
    protected boolean noche = true; // Ambiente elegido con N: true pide noche, false pide día.
    protected boolean faros = true; // Inicia los focos del auto encendidos.
    protected float factorNoche = 1; // Mezcla actual entre día (0) y noche (1); cambia poco a poco.
    protected static final float DURACION_TRANSICION = 1.2f; // Segundos que tarda el paso de día a noche.
    protected static final float ALTURA_BOMBILLA = 4.6f; // Altura desde la que cada farola emite su luz.

    /** Cada fila: X, Y, Z de la bombilla (lo que usa el shader) y X, Z del poste sobre la acera. */
    protected static final float[][] LUCES = crearFarolas();

    /**
     * Coloca una farola junto a las intersecciones alternadas como un tablero de ajedrez.
     * Así cada calle tiene una farola cerca y todos los sectores quedan iluminados.
     * El poste va en la esquina de acera de la manzana vecina y su brazo lleva la bombilla sobre la calle.
     */
    private static float[][] crearFarolas() {
        java.util.List<float[]> farolas = new java.util.ArrayList<>(); // Acumula las posiciones encontradas.
        int ultima = MAPA.length - 1; // Índice de la última fila y columna del mapa.
        for (int fila = 0; fila <= ultima; fila += 2) { // Recorre las filas de calle (pares).
            for (int columna = 0; columna <= ultima; columna += 2) { // Recorre las columnas de calle (pares).
                if ((fila / 2 + columna / 2) % 2 != 0) { // Salta una intersección de cada dos.
                    continue; // Pasa a la siguiente intersección.
                }
                int filaManzana = fila < ultima ? fila + 1 : fila - 1; // Elige la manzana de abajo, o la de arriba en el borde sur.
                int columnaManzana = columna < ultima ? columna + 1 : columna - 1; // Elige la manzana derecha, o la izquierda en el borde este.
                float haciaX = columna - columnaManzana; // Vale -1 o 1: sentido X hacia la intersección.
                float haciaZ = fila - filaManzana; // Vale -1 o 1: sentido Z hacia la intersección.
                float posteX = centro(columnaManzana) + haciaX * 4.3f; // Ubica el poste en la esquina de la acera.
                float posteZ = centro(filaManzana) + haciaZ * 4.3f; // Ubica el poste en la esquina de la acera.
                float bombillaX = posteX + haciaX * 1.0f; // El brazo adelanta la bombilla sobre el borde de la calle.
                float bombillaZ = posteZ + haciaZ * 1.0f; // El brazo adelanta la bombilla sobre el borde de la calle.
                farolas.add(new float[] {bombillaX, ALTURA_BOMBILLA, bombillaZ, posteX, posteZ}); // Guarda bombilla y poste.
            }
        }
        return farolas.toArray(new float[0][]); // Convierte la lista en la matriz usada por el shader.
    }

    // ==================== 2. CONTROLES E INDICADORES ====================

    /** Añade los interruptores de iluminación a los controles existentes. */
    @Override // Amplía el método de teclado de clase2.
    protected void tecla(int key) {
        super.tecla(key); // Conserva ESC, C y R.
        if (key == GLFW_KEY_N) { // Comprueba la tecla de cambio de ambiente.
            noche = !noche; // Pide el ambiente contrario; factorNoche lo alcanzará poco a poco.
        }
        if (key == GLFW_KEY_F) { // Comprueba el interruptor de los focos del auto.
            faros = !faros; // Enciende los faros si estaban apagados y viceversa.
        }
    }

    /** Prepara el texto que clase2 incorpora al título de la ventana. */
    @Override // Añade información al espacio reservado para indicadores.
    protected String estadoExtra() {
        String ambiente = "dia"; // Usa día como texto inicial.
        if (noche) { // Comprueba si está seleccionado el ambiente nocturno.
            ambiente = "noche"; // Reemplaza el texto por el estado real.
        }
        String estadoFaros = "OFF"; // Usa apagado como texto inicial de los focos.
        if (faros) { // Comprueba si las luces del auto están activas.
            estadoFaros = "ON"; // Indica que los faros están encendidos.
        }
        return " | N: " + ambiente + " | F: faros " + estadoFaros; // Devuelve ambos indicadores y sus teclas.
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
            cielo[canal] = dia[canal] + (oscuro[canal] - dia[canal]) * factorNoche; // Interpolación lineal.
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
        float[] cielo = colorCielo(); // Obtiene el color del cielo de este cuadro.
        vector("uCielo", cielo[0], cielo[1], cielo[2]); // La niebla lejana se funde con ese color.

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

    /** Construye una farola con cajas: base, poste, brazo diagonal, carcasa y bombilla. */
    private void dibujarFarola(float[] luz) {
        float bombillaX = luz[0]; // Posición X de la bombilla.
        float bombillaZ = luz[2]; // Posición Z de la bombilla.
        float posteX = luz[3]; // Posición X del poste sobre la acera.
        float posteZ = luz[4]; // Posición Z del poste sobre la acera.
        float brazoX = bombillaX - posteX; // Componente X del brazo, del poste a la bombilla.
        float brazoZ = bombillaZ - posteZ; // Componente Z del brazo.
        float largoBrazo = (float) Math.hypot(brazoX, brazoZ); // Longitud horizontal del brazo.
        float giro = (float) Math.atan2(-brazoZ, brazoX); // Ángulo que alinea el eje X local con el brazo.
        float metal = 0.18f; // Tono gris oscuro del metal.

        caja(posteX, 0.45f, posteZ, 0.42f, 0.3f, 0.42f, metal, metal + 0.02f, metal + 0.04f); // Base ancha sobre la acera.
        caja(posteX, 2.7f, posteZ, 0.16f, 4.5f, 0.16f, 0.22f, 0.25f, 0.29f); // Poste delgado.
        float medioX = (posteX + bombillaX) / 2; // Punto medio del brazo en X.
        float medioZ = (posteZ + bombillaZ) / 2; // Punto medio del brazo en Z.
        cajaGirada(medioX, 4.95f, medioZ, largoBrazo + 0.2f, 0.1f, 0.1f, 0.22f, 0.25f, 0.29f, giro); // Brazo hacia la calle.
        cajaGirada(bombillaX, 4.85f, bombillaZ, 0.85f, 0.2f, 0.5f, metal, metal, metal + 0.02f, giro); // Carcasa de la lámpara.
        float brilloBombilla = 0.45f; // Color apagado de la bombilla durante el día.
        if (lucesEncendidas()) { // De noche la bombilla es una fuente de luz.
            entero("uEmision", 1); // Evita que la bombilla sea oscurecida por la iluminación.
            brilloBombilla = 1; // Usa el color completo.
        }
        cajaGirada(bombillaX, 4.7f, bombillaZ, 0.6f, 0.1f, 0.34f, brilloBombilla, brilloBombilla * 0.83f, brilloBombilla * 0.5f, giro); // Bombilla cálida bajo la carcasa.
        entero("uEmision", 0); // Restablece el material normal para el siguiente objeto.
    }

    // ==================== 6. HALOS DE LUZ ====================

    /** Dibuja halos brillantes alrededor de bombillas, faros y luces de freno. */
    @Override // Sustituye el método vacío de clase1.
    protected void efectosTransparentes() {
        glEnable(GL_BLEND); // Activa la mezcla del halo con lo que ya está dibujado.
        glBlendFunc(GL_ONE, GL_ONE); // Mezcla aditiva: el halo solo suma luz, nunca oscurece.
        glDepthMask(false); // Los halos no tapan a otros halos ni a objetos posteriores.
        entero("uBrillo", 1); // Activa el modo halo en ambos shaders.
        try { // Garantiza restaurar el estado aunque ocurra un error.
            if (factorNoche > 0) { // Los halos de las farolas aparecen con la noche.
                float fuerza = factorNoche * 0.75f; // Escala el halo según el avance de la transición.
                for (float[] luz : LUCES) { // Recorre todas las bombillas.
                    caja(luz[0], 4.6f, luz[2], 3.4f, 3.4f, 3.4f, fuerza, fuerza * 0.72f, fuerza * 0.38f); // Halo cálido grande.
                }
            }
            float visibilidad = Math.max(factorNoche, 0.35f); // De día los halos del auto se ven menos.
            if (faros) { // Los faros encendidos tienen su propio halo blanco.
                for (float lado : new float[] {-0.55f, 0.55f}) { // Recorre faro izquierdo y derecho.
                    float[] punto = puntoDelAuto(lado, -1.36f); // Ubica el faro en la ciudad.
                    caja(punto[0], 0.68f, punto[1], 1.3f, 1.3f, 1.3f, visibilidad, visibilidad * 0.95f, visibilidad * 0.8f); // Halo del faro.
                }
            }
            if (frenando || faros) { // Las luces traseras brillan al frenar o con los faros encendidos.
                float rojo = frenando ? 0.9f : 0.4f * visibilidad; // El freno brilla más que la luz de posición.
                for (float lado : new float[] {-0.55f, 0.55f}) { // Recorre ambas luces traseras.
                    float[] punto = puntoDelAuto(lado, 1.38f); // Ubica la luz trasera en la ciudad.
                    caja(punto[0], 0.68f, punto[1], 1.1f, 1.1f, 1.1f, rojo, rojo * 0.06f, rojo * 0.04f); // Halo rojo.
                }
            }
        } finally { // Vuelve a la configuración de dibujo de objetos sólidos.
            entero("uBrillo", 0); // Desactiva el modo halo.
            glDepthMask(true); // Vuelve a escribir profundidad.
            glDisable(GL_BLEND); // Desactiva la mezcla.
        }
    }

    // ==================== 7. CÁLCULO DE LUZ EN LA GPU ====================

    /** Devuelve el programa GLSL que calcula el color iluminado de cada fragmento. */
    @Override // Reemplaza el shader de color plano de clase1.
    protected String fragmentShader() {
        return """
            #version 330 core
            // Selecciona la versión GLSL correspondiente a OpenGL 3.3.
            #define NUM_LUCES CANTIDAD_FAROLAS
            // Java reemplaza CANTIDAD_FAROLAS por LUCES.length antes de compilar.
            in vec3 vMundo; // Recibe la posición del fragmento en la ciudad.
            in vec3 vNormal; // Recibe la dirección perpendicular a la superficie.
            in vec3 vLocal; // Recibe la posición dentro del cubo para dibujar halos redondos.
            uniform vec3 uColor; // Recibe el color base de la caja.
            uniform vec3 uLuces[NUM_LUCES]; // Recibe las posiciones de todas las farolas.
            uniform vec3 uAuto; // Recibe la posición del auto a la altura de los faros.
            uniform vec3 uFrente; // Recibe la dirección hacia la que apunta el vehículo.
            uniform vec3 uOjo; // Recibe la posición de la cámara para brillos y niebla.
            uniform vec3 uCielo; // Recibe el color del cielo, al que tiende la niebla.
            uniform float uFactorNoche; // Vale 0 de día, 1 de noche y valores intermedios en la transición.
            uniform int uFaros; // Vale 1 cuando los focos están encendidos.
            uniform int uEmision; // Vale 1 si el objeto debe conservar su color sin oscurecerse.
            uniform int uMapa; // Vale 1 durante el dibujo del minimapa de clase4.
            uniform int uBrillo; // Vale 1 al dibujar un halo de luz.
            out vec4 color; // Entrega el color RGBA final al framebuffer.

            // Brillo especular de Blinn-Phong: reflejo de una luz hacia la cámara.
            float especular(vec3 normal, vec3 haciaLuz, vec3 haciaOjo) {
                if (dot(normal, haciaLuz) <= 0.0) { // Una cara de espaldas a la luz no refleja.
                    return 0.0; // No aporta brillo.
                }
                vec3 intermedio = normalize(haciaLuz + haciaOjo); // Vector medio entre luz y cámara.
                return pow(max(dot(normal, intermedio), 0.0), 32.0); // Cuanto más alineado, más intenso y concentrado.
            }

            void main() { // Se ejecuta para cada fragmento visible de una caja.
                if (uBrillo == 1) { // Un halo es un degradado circular que suma luz.
                    float centro = length(vLocal.xy) * 2.0; // Vale 0 en el centro y 1 en el borde del cuadrado.
                    float intensidad = pow(max(1.0 - centro, 0.0), 2.0); // Cae suavemente hacia el borde.
                    color = vec4(uColor * intensidad * 0.5, 1.0); // La mitad, porque se dibujan dos caras superpuestas.
                    return; // Termina sin calcular iluminación.
                }
                if (uEmision == 1 || uMapa == 1) { // Bombillas y minimapa usan colores directos.
                    color = vec4(uColor, 1.0); // Conserva el color base con opacidad completa.
                    return; // Termina el shader sin calcular iluminación.
                }

                float noche = uFactorNoche; // Nombre corto para la mezcla entre día y noche.
                vec3 normal = normalize(vNormal); // Convierte la normal interpolada en un vector unitario.
                vec3 haciaOjo = normalize(uOjo - vMundo); // Dirección de la superficie hacia la cámara.
                vec3 brillo = vec3(0.0); // Acumula los reflejos especulares.

                // Ambiente hemisférico: las caras que miran al cielo reciben más luz que las que miran al suelo.
                vec3 ambienteCielo = mix(vec3(0.50, 0.56, 0.66), vec3(0.17, 0.21, 0.33), noche); // Luz que baja del cielo.
                vec3 ambienteSuelo = mix(vec3(0.34, 0.31, 0.27), vec3(0.09, 0.09, 0.13), noche); // Luz que rebota del suelo.
                vec3 luz = mix(ambienteSuelo, ambienteCielo, normal.y * 0.5 + 0.5); // Mezcla según hacia dónde mira la cara.

                // Sol de día y luna de noche: una luz direccional lejana (Lambert).
                vec3 direccionSol = normalize(mix(vec3(0.4, 1.0, 0.3), vec3(-0.3, 1.0, -0.5), noche)); // La luna llega desde otro lado.
                vec3 colorSol = mix(vec3(0.72, 0.68, 0.60), vec3(0.10, 0.13, 0.22), noche); // Sol cálido, luna azulada.
                float incidenciaSol = max(dot(normal, direccionSol), 0.0); // Una cara recibe más luz si mira al sol.
                luz += colorSol * incidenciaSol; // Suma la luz difusa direccional.
                brillo += colorSol * especular(normal, direccionSol, haciaOjo) * 0.25; // Suma un reflejo suave.

                // Farolas: luz puntual que disminuye con la distancia y forma charcos de luz.
                if (noche > 0.0) { // Las farolas solo alumbran cuando cae la noche.
                    vec3 colorFarola = vec3(1.0, 0.72, 0.38); // Tono cálido de vapor de sodio.
                    for (int indice = 0; indice < NUM_LUCES; indice++) { // Acumula el aporte de cada bombilla.
                        vec3 haciaLuz = uLuces[indice] - vMundo; // Vector desde la superficie hacia la farola.
                        float distancia = length(haciaLuz); // Distancia entre superficie y bombilla.
                        vec3 direccion = haciaLuz / distancia; // Dirección unitaria hacia la luz.
                        float difusa = max(dot(normal, direccion), 0.0); // Lambert: incidencia sobre la cara.
                        float atenuacion = 1.0 / (1.0 + 0.10 * distancia + 0.035 * distancia * distancia); // Cae con la distancia.
                        float alcance = 1.0 - smoothstep(14.0, 22.0, distancia); // Corta la luz lejana con suavidad.
                        float fuerza = 4.0 * atenuacion * alcance * noche; // Intensidad final de esta farola.
                        luz += colorFarola * difusa * fuerza; // Suma la luz difusa.
                        brillo += colorFarola * especular(normal, direccion, haciaOjo) * fuerza * 0.6; // Reflejo en el asfalto.
                    }
                }

                // Faros del auto: dos focos (spotlight) con cono y alcance limitado.
                if (uFaros == 1) { // Calcula los conos únicamente si están encendidos.
                    vec3 lateral = vec3(-uFrente.z, 0.0, uFrente.x); // Dirección hacia el lado derecho del auto.
                    vec3 eje = normalize(uFrente + vec3(0.0, -0.10, 0.0)); // Inclina el foco ligeramente hacia el suelo.
                    vec3 colorFaro = vec3(1.0, 0.94, 0.72); // Luz frontal blanca y cálida.
                    for (int indice = 0; indice < 2; indice++) { // Repite el cálculo para los dos faros.
                        float separacion = indice == 0 ? -0.55 : 0.55; // Faro izquierdo o derecho.
                        vec3 origen = uAuto + uFrente * 1.36 + lateral * separacion; // Ubica el faro delante de la carrocería.
                        vec3 haciaSuperficie = vMundo - origen; // Vector del faro al fragmento.
                        float distancia = length(haciaSuperficie); // Distancia recorrida por la luz.
                        vec3 direccion = haciaSuperficie / max(distancia, 0.001); // Dirección unitaria del rayo.
                        float alineacion = dot(direccion, eje); // Cerca de 1 en el centro del haz.
                        float cono = smoothstep(0.85, 0.97, alineacion); // Borde suave del foco.
                        float difusa = max(dot(normal, -direccion), 0.0); // Cuánto mira la cara hacia el faro.
                        float atenuacion = 1.0 + 0.04 * distancia * distancia; // Disminuye la intensidad al alejarse.
                        float alcance = 1.0 - smoothstep(20.0, 28.0, distancia); // Apaga el haz entre 20 y 28 unidades.
                        float fuerza = cono * alcance * 8.0 / atenuacion; // Intensidad final de este faro.
                        luz += colorFaro * difusa * fuerza; // Suma la luz difusa del foco.
                        brillo += colorFaro * especular(normal, -direccion, haciaOjo) * fuerza * 0.4; // Reflejo del faro.
                    }
                }

                vec3 resultado = uColor * luz + brillo; // Material iluminado más los reflejos.

                // Niebla: lo lejano se funde con el cielo y da sensación de profundidad.
                float distanciaOjo = length(uOjo - vMundo); // Distancia de la cámara al fragmento.
                float densidad = mix(0.0030, 0.0050, noche); // De noche la bruma es algo más densa.
                float niebla = 1.0 - exp(-pow(distanciaOjo * densidad, 2.0)); // Crece con la distancia.
                color = vec4(mix(resultado, uCielo, niebla), 1.0); // Mezcla el color con el cielo.
            }
            """.replace("CANTIDAD_FAROLAS", String.valueOf(LUCES.length)); // Fija el tamaño del arreglo de farolas.
    }

    /** Punto de entrada de la tercera lección. */
    public static void main(String[] args) {
        clase3 aplicacion = new clase3(); // Crea la etapa con iluminación.
        aplicacion.run(); // Inicia el ciclo de vida heredado de clase1.
    }
}
