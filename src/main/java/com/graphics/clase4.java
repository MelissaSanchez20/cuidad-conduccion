package com.graphics; 

import static org.lwjgl.glfw.GLFW.*; // Permite consultar la tecla que controla el minimapa.
import static org.lwjgl.opengl.GL33.*; // Permite cambiar viewport, recorte y buffers de dibujo.

/**
 * CLASE 4: CIUDAD TERMINADA, ENTREGAS Y MINIMAPA.
 * Responsabilidad: funciones adicionales. Entregas, decoración (parques, plaza central, ventanas, pasos
 * peatonales, carteles de sectores), semáforos, tráfico autónomo, minimapa, panel en la ventana y menú de inicio/pausa. Usa la conducción de clase2 y la iluminación de clase3 sin modificarlas.
 * Orden de lectura: estado, controles, entregas, decoración y segundo pase de dibujo.
 * Hereda clase3, que hereda clase2, que a su vez hereda clase1.
 * Los semáforos son decorativos; el tráfico autónomo (sección 7) recorre rutas fijas por los carriles derechos.
 */
public class clase4 extends clase3 {

    // ==================== 1. ESTADO DEL JUEGO ====================
    private boolean mostrarMapa = true; // Muestra el minimapa desde el inicio.
    protected int entregas = 0; // Cuenta las entregas completadas; también identifica el siguiente destino.
    protected float tiempo = 0; // Acumula los segundos de la partida hasta completar el recorrido.
    protected float relojCiudad = 0; // Reloj de los semáforos: avanza siempre, también al ganar y tras reiniciar.

    protected static final int FASE_ROJO = 0; // semáforo: detenerse.
    protected static final int FASE_VERDE = 1; // semáforo: avanzar.
    protected static final int FASE_AMARILLO = 2; //  semáforo: precaución.
    protected static final float CICLO_SEMAFORO = 12; // Segundos de una vuelta completa rojo ,verde ,amarillo.
    protected static final float[][] SEMAFOROS = crearSemaforos(); // Cada fila: poste X, Z y sentido X, Z hacia el cruce.

    protected static final int ARBOL_VERDE = 0; 
    protected static final int ARBOL_SAKURA = 1; // (#FF69B4).
    protected static final int ARBOL_TAJIBO = 2; //  (#FDFBF7).
    private static final float[] COLOR_SAKURA = {1.0f, 0.412f, 0.706f}; // #FF69B4 convertido a RGB entre 0 y 1.
    private static final float[] COLOR_TAJIBO = {0.992f, 0.984f, 0.969f}; // #FDFBF7 convertido a RGB entre 0 y 1.
    private static final float CESPED = 0.37f; 
    private static final float[][] ILUMINARIAS = {{-2, -2}, {-2, 2}, {2, -2}, {2, 2}}; // Faroles de la plaza respecto a su centro.
    protected static final float[][] DESTINOS = { // Cada fila contiene X y Z de una parada sobre la calle.
        {50, -50}, // Primera entrega: esquina noreste, Barrio Norte.
        {-50, -20}, // Segunda entrega: avenida oeste, Distrito Oeste.
        {0, 10}, // Tercera entrega: frente a la plaza del Centro.
        {30, 50} // Cuarta entrega: avenida sur, Barrio Sur.
    };

    // ==================== 2. CONTROLES Y REINICIO ====================

    /** Añade la pausa y el minimapa a los controles anteriores; con un menú abierto, las teclas manejan el menú. */
    @Override // Amplía las teclas definidas por clase3.
    protected void tecla(int key) {
        if (estado != JUGANDO) { // Hay un menú abierto (inicio o pausa).
            teclaMenu(key); // Flechas, ENTER, P y ESC manejan el menú.
            return; // El resto de teclas no actúan sobre el juego.
        }
        if (key == GLFW_KEY_P) { // Pausa.
            estado = PAUSA; // Abre el menú de pausa y congela el juego.
            opcion = 0; // Selecciona CONTINUAR.
            return; // No procesa más esta tecla.
        }
        super.tecla(key); // Conserva salida, cámara, reinicio y luces.
        if (key == GLFW_KEY_M) { // Comprueba si se pulsó la tecla del mapa.
            mostrarMapa = !mostrarMapa; // Alterna entre mostrar y ocultar la vista superior.
        }
    }

    /** Reinicia el vehículo y el progreso de las entregas. */
    @Override 
    protected void reiniciar() {
        super.reiniciar(); // Restaura posición, velocidad y orientación del vehículo.
        entregas = 0; // Vuelve a seleccionar la primera parada.
        tiempo = 0; // Reinicia el cronómetro de la partida.
        trafico = crearTrafico(); // El tráfico vuelve a empezar.
        liberarCruces(); // Ningún cruce queda reservado.
    }

    // ==================== 3. REGLAS DE LAS ENTREGAS ====================

    /** Actualiza el vehículo y comprueba si llegó y frenó en el destino activo. */
    @Override // Añade el objetivo del juego al movimiento heredado.
    protected void actualizar(float deltaTime) {
        if (estado != JUGANDO) { // Menú de inicio o pausa: el juego queda congelado.
            if (estado == INICIO) { // En la portada la cámara rodea la ciudad.
                orbita += 0.12f * deltaTime; // Giro lento, independiente de los FPS.
            }
            return; // No mueve auto ni tráfico, ni avanza cronómetro o semáforos.
        }
        super.actualizar(deltaTime); // Procesa aceleración, giro, colisiones y título de la ventana.
        relojCiudad += deltaTime; // Los semáforos siguen funcionando aunque la partida haya terminado.
        avanzarTrafico(deltaTime); // El tráfico circula siempre, también después de ganar.
        if (entregas >= DESTINOS.length) { // Comprueba si ya se completaron todas las paradas.
            return; // Conserva el tiempo final y evita leer fuera del arreglo.
        }
        tiempo += deltaTime; // Suma los segundos de este cuadro al cronómetro.
        float distanciaX = autoX - DESTINOS[entregas][0]; // Calcula la separación horizontal al destino activo.
        float distanciaZ = autoZ - DESTINOS[entregas][1]; // Calcula la separación en profundidad al destino.
        float distanciaCuadrada = distanciaX * distanciaX + distanciaZ * distanciaZ; // Mide cercanía sin calcular raíz cuadrada.
        boolean estaCerca = distanciaCuadrada < 3 * 3; // Acepta un radio de llegada de tres unidades.
        boolean estaFrenando = Math.abs(velocidad) < 1; // Exige circular a menos de una unidad por segundo.
        if (estaCerca && estaFrenando) { // Solo completa la entrega si ambas condiciones se cumplen.
            entregas++; // Selecciona la siguiente parada o completa el juego.
        }
    }

    /**
     * Título del juego*/

    @Override 
    protected String textoIndicador() {
        String titulo = "Luces de Neon"; // Nombre del juego.
        if (estado == INICIO) { // Portada.
            return titulo + " | MENU"; // El juego no empezó.
        }
        if (estado == PAUSA) { 
            titulo += " | PAUSA"; // Indica la pausa.
        }
        if (entregas == DESTINOS.length) { // Todas las paradas completadas.
            return titulo + " | GANASTE en " + (int) tiempo + " s"; // Tiempo final.
        }
        return titulo + " | Entregas " + entregas + "/" + DESTINOS.length; // Progreso de la partida.
    }

    // ==================== 4. ESCENA FINAL Y DESTINO ====================

    /**  decoración y señal del destino a la escena iluminada. */
    @Override 
    protected void escena() {
        super.escena(); // Dibuja la ciudad, el auto y las farolas.
        if (!vistaMapa) { // detalles pequeños para  la vista principal.
            decorarCiudad(); // árboles, bancos, ventanas y señalización urbana.
            dibujarTrafico(); //  vehículos autónomos.
        }
        if (entregas < DESTINOS.length) { // Dibuja un objetivo únicamente mientras queden entregas.
            dibujarDestino(); // Coloca la marca dorada en la parada activa.
        }
    }

    /** Marca la próxima parada con una plataforma y una baliza flotante. */
    private void dibujarDestino() {
        float x = DESTINOS[entregas][0]; // Lee el X de la próxima entrega.
        float z = DESTINOS[entregas][1]; // Lee el Z de la próxima entrega.
        entero("uEmision", 1); // Hace que el objetivo sea visible incluso de noche.
        caja(x, 0.06f, z, 5, 0.08f, 5, 1, 0.72f, 0.12f); // Dibuja una marca dorada sobre el asfalto.
        if (vistaMapa) { // En el minimapa la ciudad se ve pequeña: el destino necesita una marca mayor.
            caja(x, 24, z, 7, 0.1f, 7, 1, 0.72f, 0.12f); // Dibuja la marca por encima de los edificios.
        } else { // Evita añadir una baliza tridimensional al mapa pequeño.
            float alturaBaliza = 3.5f + (float) Math.sin(tiempo * 2) * 0.3f; // Hace oscilar la baliza suavemente.
            cajaGirada(x, alturaBaliza, z, 0.8f, 0.8f, 0.8f, 1, 0.8f, 0.15f, tiempo); // Dibuja el cubo giratorio del objetivo.
        }
        entero("uEmision", 0); // Devuelve a los siguientes objetos su iluminación normal.
    }

    // ==================== 5. DECORACIÓN DE LA CIUDAD ====================

    /** Recorre el mapa y decide qué decoración corresponde a cada celda. */
    private void decorarCiudad() {
        for (int fila = 0; fila < MAPA.length; fila++) { // Recorre las filas del mapa.
            for (int columna = 0; columna < MAPA[fila].length; columna++) { // Recorre las columnas de esa fila.
                float x = centro(columna); // Obtiene el centro horizontal de la celda.
                float z = centro(fila); // Obtiene el centro de la celda en profundidad.
                int tipo = MAPA[fila][columna]; // Lee el contenido de la celda.
                if (tipo == 2) { // Detecta una parcela de parque.
                    dibujarParque(fila, columna, x, z); // Añade el jardín japonés.
                }
                if (tipo == 1) { // Detecta una parcela con edificio.
                    float altura = alturaEdificio(fila, columna); // Recupera la misma altura calculada en clase1.
                    dibujarVentanas(fila, columna, x, z, altura); // Coloca ventanas en sus cuatro fachadas.
                }
                if (tipo == 0 && fila % 2 != columna % 2) { // Tramo de calle entre dos intersecciones.
                    dibujarPasosPeatonales(fila % 2 == 0, x, z); // Pinta una cebra en cada extremo del tramo.
                }
            }
        }
        for (float[] semaforo : SEMAFOROS) { // Recorre las esquinas reservadas para semáforos.
            dibujarSemaforo(semaforo); // Construye poste y cabezales.
        }
        for (int indice = 0; indice < CARTELES.length; indice++) { // Pórticos con el nombre de los sectores.
            dibujarCartel(indice); // Postes, tablero y textos.
        }
    }

    // ---------- Parques ----------
    // Todo se arma con caja
    /** Elige la especie de un árbol; el patrón mezcla las tres especies en cada parque y siempre da lo mismo. */
    protected static int especieArbol(int fila, int columna, int indice) {
        return (fila + columna + indice) % 3; // 0 verde común, 1 sakura, 2 tajibo blanco.
    }

    /** Parque normal: un árbol en cada esquina y un banco en el medio. */
    private void dibujarParque(int fila, int columna, float x, float z) {
        if (fila == PLAZA && columna == PLAZA) { // El parque del centro de la ciudad es la plaza.
            dibujarPlazaCentral(x, z); // Fuente, faroles, árboles y bancos.
            return; 
        }
        float[] esquinas = {-2.8f, 2.8f}; // Distancia de los árboles al centro del parque.
        int indice = 0; // Numera los árboles para elegir su especie.
        for (float dx : esquinas) { // Izquierda y derecha.
            for (float dz : esquinas) { // Adelante y atrás.
                dibujarArbol(x + dx, z + dz, especieArbol(fila, columna, indice)); // Planta un árbol.
                indice++; // Pasa al siguiente árbol.
            }
        }
        dibujarBanco(x, z); // Banco en el centro.
    }

    /** Plaza del centro: piso de piedra, fuente, cuatro faroles, cuatro árboles y cuatro bancos. */
    private void dibujarPlazaCentral(float x, float z) {
        caja(x, CESPED + 0.015f, z, 7, 0.03f, 7, 0.80f, 0.77f, 0.70f); // Piso de piedra clara.
        dibujarFuente(x, z); // Fuente en el centro.
        for (float[] farol : ILUMINARIAS) { // Un farol en cada diagonal.
            dibujarIluminaria(x + farol[0], z + farol[1]); // Poste y foco.
        }
        dibujarArbol(x - 2.8f, z - 2.8f, ARBOL_SAKURA); // Árbol rosado.
        dibujarArbol(x + 2.8f, z + 2.8f, ARBOL_SAKURA); // Árbol rosado.
        dibujarArbol(x - 2.8f, z + 2.8f, ARBOL_TAJIBO); // Árbol blanco.
        dibujarArbol(x + 2.8f, z - 2.8f, ARBOL_TAJIBO); // Árbol blanco.
        dibujarBanco(x - 3.3f, z); // Banco oeste.
        dibujarBanco(x + 3.3f, z); // Banco este.
        dibujarBanco(x, z - 3.3f); // Banco norte.
        dibujarBanco(x, z + 3.3f); // Banco sur.
    }

    /** Fuente sencilla: pileta de piedra con agua, una columna y un chorro. De noche el agua brilla. */
    private void dibujarFuente(float x, float z) {
        caja(x, CESPED + 0.25f, z, 3.4f, 0.5f, 3.4f, 0.86f, 0.84f, 0.79f); // Pileta de piedra.
        caja(x, CESPED + 1.0f, z, 0.4f, 1.0f, 0.4f, 0.81f, 0.79f, 0.74f); // Columna central.
        if (lucesEncendidas()) { // De noche el agua está iluminada.
            entero("uEmision", 1); // El agua brilla por sí misma.
        }
        caja(x, CESPED + 0.51f, z, 3.0f, 0.02f, 3.0f, 0.25f, 0.58f, 0.88f); // Agua azul de la pileta.
        caja(x, CESPED + 1.8f, z, 0.1f, 0.6f, 0.1f, 0.70f, 0.88f, 1.0f); // Chorro de agua.
        entero("uEmision", 0); // Lo siguiente recibe luz normal.
    }

    /** Farol de la plaza: un poste oscuro y un foco que se enciende de noche. */
    private void dibujarIluminaria(float x, float z) {
        caja(x, CESPED + 0.7f, z, 0.1f, 1.4f, 0.1f, 0.15f, 0.16f, 0.18f); // Poste.
        float[] luz = {0.60f, 0.62f, 0.60f}; // Foco apagado de día (gris).
        if (lucesEncendidas()) { // De noche el foco alumbra.
            entero("uEmision", 1); // El foco es una fuente de luz.
            luz = new float[] {1.0f, 0.85f, 0.55f}; // Amarillo cálido.
        }
        caja(x, CESPED + 1.55f, z, 0.3f, 0.3f, 0.3f, luz[0], luz[1], luz[2]); // Foco.
        entero("uEmision", 0); // Lo siguiente recibe luz normal.
    }

    /** Dibuja un árbol: un tronco marrón y una copa cuadrada del color de su especie. */
    private void dibujarArbol(float x, float z, int especie) {
        caja(x, CESPED + 1.1f, z, 0.35f, 2.2f, 0.35f, 0.38f, 0.24f, 0.14f); // Tronco marrón.
        if (especie == ARBOL_SAKURA) { // Sakura: copa rosada.
            caja(x, 3.4f, z, 2.4f, 2.0f, 2.4f, COLOR_SAKURA[0], COLOR_SAKURA[1], COLOR_SAKURA[2]); // Copa rosada.
        } else if (especie == ARBOL_TAJIBO) { // Tajibo: copa blanca.
            caja(x, 3.4f, z, 2.4f, 2.0f, 2.4f, COLOR_TAJIBO[0], COLOR_TAJIBO[1], COLOR_TAJIBO[2]); // Copa blanca.
        } else { // Árbol común: copa verde.
            caja(x, 3.4f, z, 2.4f, 2.0f, 2.4f, 0.12f, 0.42f, 0.23f); // Copa verde.
        }
    }

    /** Banco de madera: patas oscuras, asiento y respaldo. */
    private void dibujarBanco(float x, float z) {
        caja(x, CESPED + 0.2f, z, 1.4f, 0.4f, 0.3f, 0.22f, 0.24f, 0.24f); // Patas.
        caja(x, CESPED + 0.45f, z, 2.0f, 0.12f, 0.6f, 0.55f, 0.30f, 0.13f); // Asiento.
        caja(x, CESPED + 0.75f, z + 0.3f, 2.0f, 0.5f, 0.1f, 0.55f, 0.30f, 0.13f); // Respaldo.
    }

    // ---------- Edificios ----------

    /** Pone tres ventanas por piso en cada fachada; de noche la mayoría se encienden. */
    private void dibujarVentanas(int fila, int columna, float x, float z, float altura) {
        int piso = 0; // Número de piso.
        for (float y = 1.7f; y + 0.45f < altura; y += 2) { // Un piso cada 2 unidades, sin tocar el techo.
            for (int ventana = -1; ventana <= 1; ventana++) { // Ventana izquierda, central y derecha.
                float d = ventana * 2; // Separación desde el centro de la fachada.
                float r = 0.24f, g = 0.32f, b = 0.42f; // Vidrio azul oscuro.
                if (lucesEncendidas() && (fila + columna + piso + ventana) % 4 != 0) { // 3 de cada 4 se encienden.
                    entero("uEmision", 1); // La ventana brilla.
                    r = 0.98f; g = 0.80f; b = 0.45f; // Luz amarilla.
                }
                caja(x + d, y, z - 3.51f, 0.8f, 0.9f, 0.04f, r, g, b); // Fachada norte.
                caja(x + d, y, z + 3.51f, 0.8f, 0.9f, 0.04f, r, g, b); // Fachada sur.
                caja(x - 3.51f, y, z + d, 0.04f, 0.9f, 0.8f, r, g, b); // Fachada oeste.
                caja(x + 3.51f, y, z + d, 0.04f, 0.9f, 0.8f, r, g, b); // Fachada este.
                entero("uEmision", 0); // Lo siguiente recibe luz normal.
            }
            piso++; // Siguiente piso.
        }
    }

    // ---------- Calles: pasos peatonales ----------

    /** Pinta un paso de cebra en cada extremo del tramo, junto a la intersección; franjas paralelas al tráfico. */
    private void dibujarPasosPeatonales(boolean traficoEnX, float x, float z) {
        for (int extremo = -1; extremo <= 1; extremo += 2) { // Extremo inicial y final del tramo.
            for (float franja = -3.6f; franja <= 3.7f; franja += 1.2f) { // Siete franjas a lo ancho de la calle.
                if (traficoEnX) { // Tramo horizontal: los autos avanzan en X.
                    caja(x + extremo * 4.1f, 0.045f, z + franja, 1.6f, 0.04f, 0.6f, 0.88f, 0.89f, 0.86f); // Franja larga en X.
                } else { // Tramo vertical: los autos avanzan en Z.
                    caja(x + franja, 0.045f, z + extremo * 4.1f, 0.6f, 0.04f, 1.6f, 0.88f, 0.89f, 0.86f); // Franja larga en Z.
                }
            }
        }
    }

    // ---------- Semáforos ----------

    /** Elige las intersecciones que no tienen farola (la otra mitad del tablero de ajedrez de clase3). */
    private static float[][] crearSemaforos() {
        java.util.List<float[]> semaforos = new java.util.ArrayList<>(); // Acumula las esquinas encontradas.
        for (int fila = 0; fila < MAPA.length; fila += 2) { // Recorre las filas de calle (pares).
            for (int columna = 0; columna < MAPA.length; columna += 2) { // Recorre las columnas de calle (pares).
                if ((fila / 2 + columna / 2) % 2 == 0) { // Esas intersecciones ya tienen farola.
                    continue; // Pasa a la siguiente intersección.
                }
                semaforos.add(esquinaAcera(fila, columna)); // Usa la esquina de acera junto al cruce.
            }
        }
        return semaforos.toArray(new float[0][]); // Convierte la lista en matriz.
    }

    /** Devuelve la fase de un semáforo: rojo 0–6 s, verde 6–10 s, amarillo 10–12 s, y vuelve a empezar. */
    protected static int faseSemaforo(float segundos) {
        float ciclo = segundos % CICLO_SEMAFORO; // Posición dentro del ciclo de 12 segundos.
        if (ciclo < 6) { // Primera parte del ciclo.
            return FASE_ROJO; // Detenerse.
        }
        if (ciclo < 10) { // Segunda parte del ciclo.
            return FASE_VERDE; // Avanzar.
        }
        return FASE_AMARILLO; // Últimos dos segundos: precaución.
    }

    /** Semáforo: poste, dos brazos cortos y dos cabezales, uno para cada calle (con fases opuestas). */
    private void dibujarSemaforo(float[] esquina) {
        float x = esquina[0]; // Poste X en la acera.
        float z = esquina[1]; // Poste Z en la acera.
        float haciaX = esquina[2]; // Hacia el cruce en X (-1 o 1).
        float haciaZ = esquina[3]; // Hacia el cruce en Z (-1 o 1).
        caja(x, 2.1f, z, 0.16f, 3.6f, 0.16f, 0.20f, 0.22f, 0.25f); // Poste.
        caja(x + haciaX * 0.2f, 3.6f, z, 0.4f, 0.1f, 0.1f, 0.20f, 0.22f, 0.25f); // Brazo en X.
        caja(x, 3.6f, z + haciaZ * 0.2f, 0.1f, 0.1f, 0.4f, 0.20f, 0.22f, 0.25f); // Brazo en Z.
        dibujarCabezal(x + haciaX * 0.45f, z, true, faseSemaforo(relojCiudad)); // Para los autos que van en Z.
        dibujarCabezal(x, z + haciaZ * 0.45f, false, faseSemaforo(relojCiudad + 6)); // Para los autos que van en X.
    }

    /** Cabezal: caja negra con tres luces (rojo arriba, amarillo, verde abajo); solo brilla la de la fase actual. */
    private void dibujarCabezal(float x, float z, boolean miraZ, int fase) {
        caja(x, 3.4f, z, 0.36f, 1.15f, 0.36f, 0.07f, 0.08f, 0.10f); // Carcasa negra.
        int[] fases = {FASE_ROJO, FASE_AMARILLO, FASE_VERDE}; // Fase de cada luz, de arriba a abajo.
        float[][] colores = {{1, 0.12f, 0.08f}, {1, 0.70f, 0.05f}, {0.15f, 1, 0.35f}}; // Rojo, amarillo y verde.
        float anchoX = miraZ ? 0.26f : 0.44f; // La luz sobresale por las dos caras que miran a la calle.
        float anchoZ = miraZ ? 0.44f : 0.26f;
        for (int i = 0; i < 3; i++) { // Las tres luces.
            float brillo = 0.18f; // Apagada: color muy oscuro.
            if (fase == fases[i]) { // Es la luz de la fase actual.
                brillo = 1; // Color completo.
                entero("uEmision", 1); // Brilla también de día.
            }
            float y = 3.76f - i * 0.36f; // Altura de esta luz.
            caja(x, y, z, anchoX, 0.26f, anchoZ, colores[i][0] * brillo, colores[i][1] * brillo, colores[i][2] * brillo); // Luz.
            entero("uEmision", 0); // Lo siguiente recibe luz normal.
        }
    }

    // ==================== 6. MINIMAPA: SEGUNDO PASE DE DIBUJO ====================

    /** Dibuja la escena principal y encima el minimapa (si está visible) y el panel. */
    @Override // Amplía el cuadro completo definido en clase1.
    protected void dibujarFrame() {
        super.dibujarFrame(); // Dibuja primero la vista normal de la ciudad.
        if (estado != INICIO) { // En la portada solo se ve la ciudad y el menú.
            if (mostrarMapa) { // El usuario puede ocultar el minimapa con M.
                dibujarMinimapa(); // Vista superior en la esquina superior derecha.
            }
            dibujarPanel(); // Panel de velocidad y luces en la esquina inferior izquierda.
        }
        if (estado != JUGANDO) { // Menú de inicio o de pausa.
            dibujarMenu(); // Se dibuja encima de todo.
        }
        if (glGetError() != GL_NO_ERROR) { // Revisa errores de OpenGL.
            throw new IllegalStateException("Error OpenGL en minimapa o panel"); // Expone el error en la consola.
        }
    }

    /** Dibuja la misma ciudad desde arriba, en un recuadro. */
    private void dibujarMinimapa() {
        int dimensionMenor = Math.min(ancho, alto); // Busca la dimensión que limita el espacio disponible.
        int lado = Math.min(280, dimensionMenor / 3); // Limita el mapa a 280 píxeles y a un tercio de la ventana.
        int margen = Math.min(18, dimensionMenor / 20); // Calcula una separación adaptable respecto a los bordes.
        int x = ancho - lado - margen; // Ubica el recuadro cerca del borde derecho.
        int y = alto - lado - margen; // Ubica el recuadro arriba; OpenGL mide Y desde abajo.
        glEnable(GL_SCISSOR_TEST); // Activa el recorte para no borrar el resto de la escena.
        glScissor(x - 3, y - 3, lado + 6, lado + 6); // Selecciona el mapa más un borde de tres píxeles.
        glClearColor(0.8f, 0.87f, 0.94f, 1); // Define un color claro para el marco.
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // Borra solo el recuadro exterior gracias al scissor.
        glScissor(x, y, lado, lado); // Reduce el recorte al interior del minimapa.
        glClearColor(0.06f, 0.10f, 0.15f, 1); // Define el fondo oscuro del mapa.
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // Limpia color y profundidad dentro del mapa.
        glViewport(x, y, lado, lado); // Redirige la proyección al recuadro cuadrado.
        entero("uMapa", 1); // Vista desde arriba.
        decimal("uEscalaMapa", LIMITE + 6); // Deja un margen arriba para la marca del norte.
        vistaMapa = true; // Indica a escena() que omita decoración pequeña y baliza flotante.
        try { // Restaura la vista aunque falle.
            escena(); // Dibuja otra vez la misma ciudad, ahora vista desde arriba.
            dibujarIndicadorAuto(); // Resalta la posición y el frente del jugador en el mapa.
            dibujarNorte(); // Escribe una N en el borde superior: el norte (-Z) queda arriba.
            dibujarTraficoEnMapa(); // Marca cada vehículo autónomo con su color.
        } finally { // El siguiente cuadro debe volver a la configuración de pantalla completa.
            vistaMapa = false; // Reactiva los detalles de la escena principal.
            entero("uMapa", 0); // Recupera la proyección en perspectiva.
            glDisable(GL_SCISSOR_TEST); // Permite que la próxima limpieza abarque toda la pantalla.
            glViewport(0, 0, ancho, alto); // Recupera el área de dibujo de la ventana completa.
        }
    }

    /** Dibuja una marca cian y una punta blanca por encima de los edificios del minimapa. */
    private void dibujarIndicadorAuto() {
        cajaGirada(autoX, 25, autoZ, 3.2f, 0.1f, 4.6f, 0.1f, 1, 1, angulo); // Marca la posición con un rectángulo cian orientado.
        float frenteX = -(float) Math.sin(angulo); // Calcula la dirección frontal en el eje X.
        float frenteZ = -(float) Math.cos(angulo); // Calcula la dirección frontal en el eje Z.
        float puntaX = autoX + frenteX * 2.8f; // Desplaza la punta hacia delante en X.
        float puntaZ = autoZ + frenteZ * 2.8f; // Desplaza la punta hacia delante en Z.
        caja(puntaX, 26, puntaZ, 1.3f, 0.1f, 1.3f, 1, 1, 1); // Dibuja la punta blanca encima del indicador cian.
    }

    /** Dibuja la letra N con tres cajas en el margen superior del minimapa. */
    private void dibujarNorte() {
        float z = -LIMITE - 3; // Centro de la letra, fuera del borde norte de la ciudad.
        caja(-1, 27, z, 0.5f, 0.1f, 3.4f, 1, 1, 1); // Trazo vertical izquierdo.
        caja(1, 27, z, 0.5f, 0.1f, 3.4f, 1, 1, 1); // Trazo vertical derecho.
        float giro = (float) Math.atan2(2, 3.4); // Inclina la diagonal de arriba-izquierda a abajo-derecha.
        cajaGirada(0, 27, z, 0.5f, 0.1f, (float) Math.hypot(2, 3.4), 1, 1, 1, giro); // Trazo diagonal.
    }

    // ==================== 7. TRÁFICO AUTÓNOMO ====================

    protected static final int AUTO_CELESTE = 0; // Auto celeste.
    protected static final int AUTO_AMARILLO = 1; // Auto amarillo.
    protected static final int AUTO_VERDE = 2; // Auto verde.
    private static final float CARRIL = 2.5f; // Distancia del centro de la calle al centro del carril derecho.
    private static final float ZONA_CRUCE = 6; // Distancia a un cruce desde la que hay que reservarlo.
    protected static final float RADIO_VEHICULO = 1.4f; // Radio de colisión de cada vehículo autónomo.
    private static final int NODOS = MAPA.length / 2 + 1; // Intersecciones por lado: calles en filas y columnas pares.
    private static final int[][][] RUTAS = { // Esquinas de cada ruta cerrada: columna y fila de la intersección (0 a 5).
        {{1, 1}, {4, 1}, {4, 4}, {1, 4}}, // Auto celeste: anillo alrededor del centro.
        {{0, 0}, {3, 0}, {3, 3}, {5, 3}, {5, 5}, {2, 5}, {2, 2}, {0, 2}}, // Auto amarillo: recorrido con ocho giros.
        {{1, 2}, {4, 2}, {4, 5}, {1, 5}} // Auto verde: rectángulo hacia el Barrio Sur.
    };
    private static final int[] MODELOS = {AUTO_CELESTE, AUTO_AMARILLO, AUTO_VERDE}; // Modelo de cada ruta.
    private static final float[] VELOCIDADES = {10, 8, 6}; // Unidades por segundo de cada vehículo.
    private static final float[][] COLORES_MAPA = {{0.53f, 0.81f, 0.92f}, {1.0f, 0.80f, 0.08f}, {0.30f, 0.85f, 0.40f}}; // Marca de cada modelo en el minimapa.
    private static final float[][] COLORES_AUTO = {{0.53f, 0.81f, 0.92f}, {1.0f, 0.80f, 0.08f}, {0.20f, 0.50f, 0.28f}}; // Carrocería de cada modelo: celeste, amarillo y verde.

    /** Estado de un vehículo autónomo: su ruta por los carriles y su posición actual. */
    protected static class Vehiculo {
        int modelo; // AUTO_CELESTE, AUTO_AMARILLO o AUTO_VERDE.
        float velocidad; // Unidades por segundo cuando avanza.
        int[] nodos; // Número de cada cruce de la ruta.
        float[][] puntos; // Punto de paso X, Z sobre el carril derecho en cada intersección.
        int tramo; // Índice del punto de partida del tramo actual.
        float avance; // Distancia recorrida dentro del tramo.
        float x; // Posición X actual.
        float z; // Posición Z actual.
        float dirX; // Dirección X del tramo actual.
        float dirZ; // Dirección Z del tramo actual.
        float angulo; // Hacia dónde mira el auto.
        boolean detenido; // true si está esperando: enciende las luces de freno.
        int vueltas; // Vueltas completas a la ruta.
    }

    protected Vehiculo[] trafico = crearTrafico(); // Vehículos que circulan por la ciudad.
    private final int[] reservas = new int[NODOS * NODOS]; // Vehículo que ocupa cada cruce, o -1 si está libre.

    {
        liberarCruces(); // Al crear el juego todos los cruces están libres.
    }

    /** Marca todos los cruces como libres. */
    private void liberarCruces() {
        java.util.Arrays.fill(reservas, -1); // -1 significa que ningún vehículo lo ocupa.
    }

    /** Crea los vehículos a mitad del primer tramo de su ruta. */
    protected static Vehiculo[] crearTrafico() {
        Vehiculo[] vehiculos = new Vehiculo[RUTAS.length]; // Un vehículo por ruta.
        for (int indice = 0; indice < RUTAS.length; indice++) { // Recorre las rutas.
            Vehiculo vehiculo = new Vehiculo(); // Crea el vehículo.
            vehiculo.modelo = MODELOS[indice]; // Asigna su modelo.
            vehiculo.velocidad = VELOCIDADES[indice]; // Asigna su velocidad.
            int[][] ruta = expandirRuta(RUTAS[indice]); // Incluye cada intersección por la que pasa.
            vehiculo.nodos = new int[ruta.length]; // Reserva los identificadores.
            vehiculo.puntos = new float[ruta.length][]; // Reserva los puntos de paso.
            for (int paso = 0; paso < ruta.length; paso++) { // Recorre la ruta.
                vehiculo.nodos[paso] = ruta[paso][1] * NODOS + ruta[paso][0]; // Identificador del cruce.
                vehiculo.puntos[paso] = puntoDeCarril(ruta, paso); // Punto sobre el carril derecho.
            }
            vehiculo.avance = largoTramo(vehiculo) / 2; // Empieza a mitad del primer tramo, lejos de los cruces.
            ubicar(vehiculo); // Calcula posición y dirección.
            vehiculo.angulo = (float) Math.atan2(-vehiculo.dirX, -vehiculo.dirZ); // Mira hacia donde avanza.
            vehiculos[indice] = vehiculo; // Guarda el vehículo.
        }
        return vehiculos; // Entrega el tráfico inicial.
    }

    /** Convierte las esquinas de una ruta en la lista de todas las intersecciones que recorre. */
    private static int[][] expandirRuta(int[][] esquinas) {
        java.util.List<int[]> nodos = new java.util.ArrayList<>(); // Acumula las intersecciones.
        for (int indice = 0; indice < esquinas.length; indice++) { // Recorre cada lado de la ruta.
            int[] desde = esquinas[indice]; // Esquina de partida.
            int[] hasta = esquinas[(indice + 1) % esquinas.length]; // Esquina siguiente (la última vuelve a la primera).
            int pasoColumna = Integer.signum(hasta[0] - desde[0]); // -1, 0 o 1 en columnas.
            int pasoFila = Integer.signum(hasta[1] - desde[1]); // -1, 0 o 1 en filas.
            int columna = desde[0]; // Columna actual.
            int fila = desde[1]; // Fila actual.
            while (columna != hasta[0] || fila != hasta[1]) { // Avanza hasta la esquina siguiente.
                nodos.add(new int[] {columna, fila}); // Guarda la intersección.
                columna += pasoColumna; // Pasa a la siguiente columna.
                fila += pasoFila; // Pasa a la siguiente fila.
            }
        }
        return nodos.toArray(new int[0][]); // Entrega la ruta completa.
    }

    /** Calcula el punto del carril derecho en una intersección, según de dónde viene y a dónde va el vehículo. */
    private static float[] puntoDeCarril(int[][] ruta, int paso) {
        int[] anterior = ruta[(paso - 1 + ruta.length) % ruta.length]; // Intersección anterior.
        int[] actual = ruta[paso]; // Esta intersección.
        int[] siguiente = ruta[(paso + 1) % ruta.length]; // Intersección siguiente.
        int entradaX = Integer.signum(actual[0] - anterior[0]); // Dirección de llegada en X.
        int entradaZ = Integer.signum(actual[1] - anterior[1]); // Dirección de llegada en Z.
        int salidaX = Integer.signum(siguiente[0] - actual[0]); // Dirección de salida en X.
        int salidaZ = Integer.signum(siguiente[1] - actual[1]); // Dirección de salida en Z.
        float x = centro(actual[0] * 2); // Centro X de la intersección.
        float z = centro(actual[1] * 2); // Centro Z de la intersección.
        x += -entradaZ * CARRIL; // La derecha de la dirección (dx, dz) es (-dz, dx).
        z += entradaX * CARRIL; // Carril derecho de la calle de llegada.
        if (entradaX != salidaX || entradaZ != salidaZ) { // En un giro también cuenta el carril de salida.
            x += -salidaZ * CARRIL; // Carril derecho de la calle de salida.
            z += salidaX * CARRIL; // El punto queda donde se cruzan ambos carriles.
        }
        return new float[] {x, z}; // Punto de paso.
    }

    /** Largo del tramo actual de un vehículo. */
    private static float largoTramo(Vehiculo vehiculo) {
        float[] desde = vehiculo.puntos[vehiculo.tramo]; // Punto de partida.
        float[] hasta = vehiculo.puntos[(vehiculo.tramo + 1) % vehiculo.puntos.length]; // Punto de llegada.
        return (float) Math.hypot(hasta[0] - desde[0], hasta[1] - desde[1]); // Distancia entre ambos.
    }

    /** Calcula posición y dirección a partir del tramo y el avance. */
    private static void ubicar(Vehiculo vehiculo) {
        float[] desde = vehiculo.puntos[vehiculo.tramo]; // Punto de partida.
        float[] hasta = vehiculo.puntos[(vehiculo.tramo + 1) % vehiculo.puntos.length]; // Punto de llegada.
        float largo = largoTramo(vehiculo); // Largo del tramo.
        vehiculo.dirX = (hasta[0] - desde[0]) / largo; // Dirección unitaria en X.
        vehiculo.dirZ = (hasta[1] - desde[1]) / largo; // Dirección unitaria en Z.
        vehiculo.x = desde[0] + vehiculo.dirX * vehiculo.avance; // Posición X sobre el tramo.
        vehiculo.z = desde[1] + vehiculo.dirZ * vehiculo.avance; // Posición Z sobre el tramo.
    }

    /** Mueve todos los vehículos con deltaTime, respetando cruces reservados, a los de delante y al jugador. */
    protected void avanzarTrafico(float deltaTime) {
        for (int indice = 0; indice < trafico.length; indice++) { // Recorre los vehículos.
            Vehiculo vehiculo = trafico[indice]; // Vehículo actual.
            float largo = largoTramo(vehiculo); // Largo del tramo actual.
            int cantidad = vehiculo.nodos.length; // Intersecciones de la ruta.
            int cruceAnterior = vehiculo.nodos[vehiculo.tramo]; // Cruce del que salió.
            if (vehiculo.avance > ZONA_CRUCE && reservas[cruceAnterior] == indice) { // Ya se alejó de ese cruce.
                reservas[cruceAnterior] = -1; // Lo deja libre para los demás.
            }
            boolean puedeAvanzar = true; // Supone que el camino está libre.
            int cruceSiguiente = vehiculo.nodos[(vehiculo.tramo + 1) % cantidad]; // Cruce al que se acerca.
            if (largo - vehiculo.avance < ZONA_CRUCE) { // Está por entrar al cruce.
                if (reservas[cruceSiguiente] == -1) { // Nadie lo ocupa.
                    reservas[cruceSiguiente] = indice; // Lo reserva.
                }
                if (reservas[cruceSiguiente] != indice) { // Otro vehículo lo ocupa.
                    puedeAvanzar = false; // Espera antes de entrar.
                }
            }
            if (hayAlguienDelante(indice)) { // Otro vehículo o el jugador están muy cerca por delante.
                puedeAvanzar = false; // Espera.
            }
            vehiculo.detenido = !puedeAvanzar; // Las luces de freno muestran la espera.
            if (puedeAvanzar) { // El camino está libre.
                vehiculo.avance += vehiculo.velocidad * deltaTime; // Avanza según el tiempo transcurrido.
                while (vehiculo.avance >= largo) { // Pasó el final del tramo.
                    vehiculo.avance -= largo; // Conserva lo que sobró.
                    vehiculo.tramo = (vehiculo.tramo + 1) % cantidad; // Pasa al tramo siguiente.
                    if (vehiculo.tramo == 0) { // Volvió al inicio de la ruta.
                        vehiculo.vueltas++; // Cuenta una vuelta completa.
                    }
                    largo = largoTramo(vehiculo); // Largo del nuevo tramo.
                }
            }
            ubicar(vehiculo); // Actualiza posición y dirección.
            float objetivo = (float) Math.atan2(-vehiculo.dirX, -vehiculo.dirZ); // Orientación del tramo.
            float diferencia = objetivo - vehiculo.angulo; // Cuánto falta girar.
            diferencia = (float) Math.atan2(Math.sin(diferencia), Math.cos(diferencia)); // Gira por el lado más corto.
            float maximo = 6 * deltaTime; // Gira como mucho 6 radianes por segundo.
            vehiculo.angulo += Math.max(-maximo, Math.min(maximo, diferencia)); // Giro suave en las esquinas.
        }
    }

    /** Indica si otro vehículo que va en el mismo sentido, o el jugador, está justo delante. */
    private boolean hayAlguienDelante(int indice) {
        Vehiculo vehiculo = trafico[indice]; // Vehículo que pregunta.
        for (int otro = 0; otro < trafico.length; otro++) { // Revisa los demás vehículos.
            Vehiculo delante = trafico[otro]; // Posible vehículo de delante.
            float mismoSentido = delante.dirX * vehiculo.dirX + delante.dirZ * vehiculo.dirZ; // 1 mismo sentido, -1 contrario.
            if (otro != indice && mismoSentido > 0.3f && estaDelante(vehiculo, delante.x, delante.z, 5.5f, 1.8f)) { // Lo sigue de cerca.
                return true; // Debe esperar.
            }
        }
        return estaDelante(vehiculo, autoX, autoZ, 6, 3); // El jugador bloquea su carril.
    }

    /** Comprueba si un punto está delante del vehículo, a menos de cierta distancia y desvío lateral. */
    private static boolean estaDelante(Vehiculo vehiculo, float x, float z, float distancia, float lateral) {
        float haciaX = x - vehiculo.x; // Vector hacia el punto en X.
        float haciaZ = z - vehiculo.z; // Vector hacia el punto en Z.
        float adelante = haciaX * vehiculo.dirX + haciaZ * vehiculo.dirZ; // Distancia en la dirección de avance.
        float costado = Math.abs(haciaX * vehiculo.dirZ - haciaZ * vehiculo.dirX); // Distancia hacia los lados.
        return adelante > 0 && adelante < distancia && costado < lateral; // Está en el carril, por delante.
    }

    /** Impide que el jugador atraviese un vehículo, pero le deja alejarse si ya están en contacto. */
    @Override // Amplía las colisiones de clase2 con el tráfico.
    protected boolean puedeCircular(float x, float z) {
        if (!super.puedeCircular(x, z)) { // Manzanas y bordes del mapa.
            return false; // Rechaza la posición.
        }
        float minimo = RADIO_AUTO + RADIO_VEHICULO; // Distancia mínima entre centros.
        for (Vehiculo vehiculo : trafico) { // Revisa cada vehículo.
            float nueva = (float) Math.hypot(x - vehiculo.x, z - vehiculo.z); // Distancia en la posición propuesta.
            float actual = (float) Math.hypot(autoX - vehiculo.x, autoZ - vehiculo.z); // Distancia en la posición actual.
            if (nueva < minimo && nueva < actual) { // Se acercaría a un vehículo con el que choca.
                return false; // Rechaza el movimiento.
            }
        }
        return true; // Acepta la posición.
    }

    // ---------- Dibujo del tráfico ----------

    /** Dibuja cada vehículo con el color de su modelo. */
    private void dibujarTrafico() {
        for (Vehiculo vehiculo : trafico) { // Recorre los vehículos.
            float[] color = COLORES_AUTO[vehiculo.modelo]; // Color de la carrocería.
            dibujarVehiculo(vehiculo, color[0], color[1], color[2]); // Auto sencillo de ese color.
        }
    }

    /** Atajo para dibujar una pieza local de un vehículo autónomo. */
    private void piezaDe(Vehiculo v, float x, float y, float z, float sx, float sy, float sz, float r, float g, float b) {
        piezaEn(v.x, v.z, v.angulo, x, y, z, sx, sy, sz, r, g, b); // Usa la posición y orientación del vehículo.
    }

    /** Dibuja un faro delantero: brilla de noche. */
    private void faro(Vehiculo v, float x) {
        float brillo = 0.85f; // Faro apagado de día.
        if (lucesEncendidas()) { // De noche los faros están encendidos.
            entero("uEmision", 1); // Fuente de luz.
            brillo = 1; // Color completo.
        }
        piezaDe(v, x, 0.68f, -1.32f, 0.38f, 0.2f, 0.07f, brillo, brillo * 0.95f, brillo * 0.65f); // Faro.
        entero("uEmision", 0); // Restablece la iluminación normal.
    }

    /** Dibuja una luz trasera: fuerte al frenar, tenue de noche. */
    private void trasera(Vehiculo v, float x) {
        float rojo = 0.85f; // Luz trasera apagada.
        if (v.detenido) { // Está esperando: luz de freno.
            entero("uEmision", 1); // Fuente de luz.
            rojo = 1; // Rojo intenso.
        } else if (lucesEncendidas()) { // Luz de posición nocturna.
            entero("uEmision", 1); // Fuente de luz.
            rojo = 0.55f; // Rojo suave.
        }
        piezaDe(v, x, 0.68f, 1.32f, 0.35f, 0.17f, 0.07f, rojo, 0.05f, 0.05f); // Luz trasera.
        entero("uEmision", 0); // Restablece la iluminación normal.
    }

    /** Auto sencillo, igual al del jugador: carrocería del color pedido, cabina azulada, ruedas y luces. */
    private void dibujarVehiculo(Vehiculo v, float r, float g, float b) {
        piezaDe(v, 0, 0.65f, 0, 1.65f, 0.55f, 2.6f, r, g, b); // Carrocería.
        piezaDe(v, 0, 1.12f, 0.12f, 1.3f, 0.55f, 1.25f, 0.22f, 0.65f, 0.78f); // Cabina azulada.
        for (int lado = -1; lado <= 1; lado += 2) { // Izquierda y derecha.
            for (int eje = -1; eje <= 1; eje += 2) { // Eje delantero y trasero.
                piezaDe(v, lado * 0.88f, 0.38f, eje * 0.82f, 0.24f, 0.58f, 0.6f, 0.055f, 0.065f, 0.08f); // Rueda oscura.
            }
            faro(v, lado * 0.55f); // Faro delantero.
            trasera(v, lado * 0.55f); // Luz trasera.
        }
    }

    /** Marca cada vehículo en el minimapa con un rectángulo de su color, orientado. */
    private void dibujarTraficoEnMapa() {
        for (Vehiculo v : trafico) { // Recorre los vehículos.
            float[] color = COLORES_MAPA[v.modelo]; // Color del modelo.
            cajaGirada(v.x, 24.5f, v.z, 2.4f, 0.1f, 3.4f, color[0], color[1], color[2], v.angulo); // Marca orientada.
        }
    }

    // ==================== 8. PANEL EN LA VENTANA ====================

    private static final float PANEL_ANCHO = 290; // Ancho del panel en píxeles de diseño (ventana de 760 px de alto).
    private static final float PANEL_ALTO = 144; // Alto del panel en píxeles de diseño (fila del sector arriba).
    private static final float ESCALA_LIENZO = 100; // Mitad del espacio ortográfico usado para dibujar en 2D.
    private static final float VELOCIDAD_MAXIMA_KMH = 16 * 3.6f; // Velocidad máxima del auto (16 u/s) en km/h.
    private float lienzoAncho = PANEL_ANCHO; // Ancho en píxeles de diseño del recuadro 2D actual.
    private float lienzoAlto = PANEL_ALTO; // Alto en píxeles de diseño del recuadro 2D actual.

    /**
     * Fuente de píxeles de 3 × 5: cada carácter son 15 dígitos (5 filas de 3, de arriba abajo); 1 = píxel encendido.
     * Permite escribir texto con las mismas cajas de siempre, sin texturas.
     */
    protected static final java.util.Map<Character, String> FUENTE = new java.util.HashMap<>();

    static {
        String[] glifos = { // Carácter seguido de sus 15 píxeles.
            "0111101101101111", "1010110010010111", "2111001111100111", "3111001111001111", "4101101111001001",
            "5111100111001111", "6111100111101111", "7111001001001001", "8111101111101111", "9111101111001111",
            "A010101111101101", "B110101110101110", "C011100100100011", "D110101101101110", "E111100110100111",
            "F111100110100100", "G011100101101011", "H101101111101101", "I111010010010111", "J001001001101010",
            "K101101110101101", "L100100100100111", "M101111111101101", "N111101101101101", "O010101101101010",
            "P110101110100100", "Q010101101110011", "R110101110101101", "S011100010001110", "T111010010010010",
            "U101101101101111", "V101101101101010", "W101101111111101", "X101101010101101", "Y101101010010010",
            "Z111001010100111", "/001001010100100", ">100010001010100", ":000010000010000", "-000000111000000",
            ".000000000000010", " 000000000000000"
        };
        for (String glifo : glifos) { // Recorre la tabla.
            FUENTE.put(glifo.charAt(0), glifo.substring(1)); // Guarda los 15 píxeles del carácter.
        }
    }

    /** Ancho en píxeles de diseño de un texto: cada carácter ocupa 3 píxeles más 1 de separación. */
    protected static float anchoTexto(String texto, float pixel) {
        return Math.max(0, texto.length() * 4 - 1) * pixel; // Sin separación después del último carácter.
    }

    /**
     * Prepara un recuadro de la ventana para dibujar en 2D: (x, y, ancho, alto) en píxeles reales y
     * un tamaño de diseño con el que se miden los rectángulos y textos. Reutiliza la proyección plana del minimapa.
     */
    private void abrirLienzo(int x, int y, int anchoPx, int altoPx, float anchoDiseno, float altoDiseno) {
        lienzoAncho = anchoDiseno; // Unidades horizontales del recuadro.
        lienzoAlto = altoDiseno; // Unidades verticales del recuadro.
        glEnable(GL_SCISSOR_TEST); // Limita la limpieza al recuadro.
        glScissor(x, y, anchoPx, altoPx); // Recuadro elegido.
        glClear(GL_DEPTH_BUFFER_BIT); // Solo borra profundidad: lo de atrás sigue visible.
        glViewport(x, y, anchoPx, altoPx); // Dibuja dentro del recuadro.
        entero("uMapa", 1); // Proyección ortográfica plana.
        decimal("uEscalaMapa", ESCALA_LIENZO); // Espacio de -100 a 100 en ambos ejes.
    }

    /** Devuelve OpenGL al dibujo normal de la ventana completa. */
    private void cerrarLienzo() {
        glDisable(GL_BLEND); // Sin mezcla.
        entero("uMapa", 0); // Perspectiva normal.
        glDisable(GL_SCISSOR_TEST); // Sin recorte.
        glViewport(0, 0, ancho, alto); // Ventana completa.
    }

    /** Dibuja un rectángulo en píxeles de diseño del lienzo (origen abajo a la izquierda); capa ordena lo que va encima. */
    private void rectangulo(float px, float py, float pw, float ph, float r, float g, float b, float capa) {
        float centroX = (px + pw / 2) / lienzoAncho * 2 - 1; // Centro horizontal entre -1 y 1.
        float centroY = (py + ph / 2) / lienzoAlto * 2 - 1; // Centro vertical entre -1 y 1.
        float anchoCaja = pw / lienzoAncho * 2 * ESCALA_LIENZO; // Ancho en el espacio ortográfico.
        float altoCaja = ph / lienzoAlto * 2 * ESCALA_LIENZO; // Alto en el espacio ortográfico.
        caja(centroX * ESCALA_LIENZO, capa, -centroY * ESCALA_LIENZO, anchoCaja, 0.1f, altoCaja, r, g, b); // El mapa pone -Z arriba.
    }

    /** Dibuja un rectángulo semitransparente: alfa es la opacidad entre 0 y 1. */
    private void rectanguloTranslucido(float px, float py, float pw, float ph, float r, float g, float b, float alfa) {
        glEnable(GL_BLEND); // Mezcla con lo que ya está dibujado.
        glBlendColor(0, 0, 0, alfa); // Opacidad constante.
        glBlendFunc(GL_CONSTANT_ALPHA, GL_ONE_MINUS_CONSTANT_ALPHA); // Color nuevo × alfa + fondo × (1 - alfa).
        rectangulo(px, py, pw, ph, r, g, b, 1); // Capa más baja: todo lo demás va encima.
        glDisable(GL_BLEND); // El contenido es opaco.
    }

    /** Escribe un texto con la fuente de píxeles; (px, py) es su esquina inferior izquierda. */
    private void texto(String texto, float px, float py, float pixel, float r, float g, float b) {
        for (int indice = 0; indice < texto.length(); indice++) { // Recorre los caracteres.
            String glifo = FUENTE.getOrDefault(Character.toUpperCase(texto.charAt(indice)), FUENTE.get(' ')); // Píxeles del carácter.
            float inicioX = px + indice * 4 * pixel; // Posición del carácter.
            for (int fila = 0; fila < 5; fila++) { // Filas de arriba abajo.
                for (int columna = 0; columna < 3; columna++) { // Columnas de izquierda a derecha.
                    if (glifo.charAt(fila * 3 + columna) == '1') { // Píxel encendido.
                        rectangulo(inicioX + columna * pixel, py + (4 - fila) * pixel, pixel, pixel, r, g, b, 30); // Lo dibuja.
                    }
                }
            }
        }
    }

    /** Escribe un texto centrado horizontalmente en el lienzo. */
    private void textoCentrado(String texto, float py, float pixel, float r, float g, float b) {
        texto(texto, (lienzoAncho - anchoTexto(texto, pixel)) / 2, py, pixel, r, g, b); // Centra según su ancho.
    }

    /** Dibuja un borde fino alrededor de todo el lienzo. */
    private void marco(float borde, float claro) {
        rectangulo(0, 0, lienzoAncho, borde, claro, claro + 0.05f, claro + 0.12f, 10); // Borde inferior.
        rectangulo(0, lienzoAlto - borde, lienzoAncho, borde, claro, claro + 0.05f, claro + 0.12f, 10); // Borde superior.
        rectangulo(0, 0, borde, lienzoAlto, claro, claro + 0.05f, claro + 0.12f, 10); // Borde izquierdo.
        rectangulo(lienzoAncho - borde, 0, borde, lienzoAlto, claro, claro + 0.05f, claro + 0.12f, 10); // Borde derecho.
    }

    /** Dibuja el panel con la velocidad, una barra de velocidad y los indicadores de luces. */
    private void dibujarPanel() {
        float escala = Math.max(0.7f, Math.min(1.6f, alto / 760f)); // El panel crece o se achica con la ventana.
        int anchoPx = Math.round(PANEL_ANCHO * escala); // Ancho real.
        int altoPx = Math.round(PANEL_ALTO * escala); // Alto real.
        int margen = Math.round(18 * escala); // Separación respecto a los bordes.
        if (anchoPx + margen * 2 > ancho || altoPx + margen * 2 > alto) { // Ventana demasiado pequeña.
            return; // No dibuja el panel antes que tapar la escena.
        }
        abrirLienzo(margen, margen, anchoPx, altoPx, PANEL_ANCHO, PANEL_ALTO); // Esquina inferior izquierda.
        try { // Restaura el estado aunque falle el dibujo.
            rectanguloTranslucido(0, 0, PANEL_ANCHO, PANEL_ALTO, 0.03f, 0.05f, 0.09f, 0.72f); // Fondo oscuro al 72 %.
            marco(2, 0.55f); // Borde claro.
            rectangulo(166, 12, 1.5f, 96, 0.3f, 0.33f, 0.4f, 10); // Separador entre velocidad y luces.
            rectangulo(12, 116, PANEL_ANCHO - 24, 1.5f, 0.3f, 0.33f, 0.4f, 10); // Separador bajo la fila del sector.
            String sectorActual = SECTORES[sectorEn(autoX, autoZ)].toUpperCase(); // Sector por el que circula el auto.
            texto("SECTOR:", 14, 124, 2.5f, 0.6f, 0.68f, 0.8f); // Etiqueta.
            texto(sectorActual, 14 + anchoTexto("SECTOR: ", 2.5f), 124, 2.5f, 0.55f, 0.95f, 0.65f); // Nombre en verde.
            dibujarVelocidadPanel(); // Número grande, unidad y barra.
            dibujarLucesPanel(); // Faros, ambiente, freno y mapa.
        } finally { // Vuelve a la vista principal.
            cerrarLienzo(); // Restaura viewport, recorte y proyección.
        }
    }

    /** Velocidad en grande, la unidad, la marcha atrás y una barra que cambia de color. */
    private void dibujarVelocidadPanel() {
        texto("VELOCIDAD", 14, 98, 2, 0.6f, 0.68f, 0.8f); // Título.
        String numero = String.valueOf(kilometrosPorHora()); // Velocidad redondeada, igual que en el título.
        texto(numero, 14, 44, 8, 1, 1, 1); // Número grande.
        float finNumero = 14 + anchoTexto(numero, 8); // Dónde termina el número.
        texto("KM/H", finNumero + 10, 44, 2.5f, 0.75f, 0.8f, 0.9f); // Unidad junto al número.
        if (velocidad < -0.3f) { // El auto retrocede.
            texto("REV", finNumero + 10, 66, 2.5f, 1, 0.55f, 0.1f); // Indica marcha atrás.
        }
        float fraccion = Math.min(1, kilometrosPorHora() / VELOCIDAD_MAXIMA_KMH); // Parte de la barra que se llena.
        float[] color = {0.25f, 0.85f, 0.4f}; // Verde a baja velocidad.
        if (fraccion > 0.8f) { // Muy rápido.
            color = new float[] {1, 0.3f, 0.2f}; // Rojo.
        } else if (fraccion > 0.5f) { // Rápido.
            color = new float[] {1, 0.8f, 0.15f}; // Amarillo.
        }
        rectangulo(14, 20, 140, 10, 0.18f, 0.2f, 0.25f, 20); // Fondo de la barra.
        if (fraccion > 0) { // Hay velocidad que mostrar.
            rectangulo(14, 20, 140 * fraccion, 10, color[0], color[1], color[2], 25); // Parte llena.
        }
    }

    /** Cuatro indicadores: faros, día o noche, freno y minimapa. */
    private void dibujarLucesPanel() {
        texto("LUCES", 180, 98, 2, 0.6f, 0.68f, 0.8f); // Título.
        float[] apagado = {0.22f, 0.24f, 0.28f}; // Lámpara apagada.
        indicador(78, faros ? new float[] {1, 0.9f, 0.35f} : apagado, faros ? "FAROS ON" : "FAROS OFF"); // Faros (tecla F).
        if (noche) { // Ambiente elegido con N.
            indicador(56, new float[] {0.45f, 0.6f, 1}, "NOCHE"); // Luna azulada.
        } else { // Día.
            indicador(56, new float[] {1, 0.65f, 0.15f}, "DIA"); // Sol anaranjado.
        }
        indicador(34, frenando ? new float[] {1, 0.15f, 0.1f} : apagado, "FRENO"); // Luces de freno.
        indicador(12, mostrarMapa ? new float[] {0.3f, 0.9f, 0.5f} : apagado, mostrarMapa ? "MAPA ON" : "MAPA OFF"); // Minimapa (tecla M).
    }

    /** Dibuja una lámpara cuadrada de color y su etiqueta. */
    private void indicador(float py, float[] color, String etiqueta) {
        rectangulo(180, py, 12, 12, color[0], color[1], color[2], 20); // Lámpara.
        texto(etiqueta, 200, py + 1, 2, 0.9f, 0.92f, 0.95f); // Etiqueta.
    }

    // ==================== 9. MENÚ DE INICIO Y PAUSA ====================

    protected static final int INICIO = 0; // Portada: el juego todavía no empezó.
    protected static final int JUGANDO = 1; // Partida en curso.
    protected static final int PAUSA = 2; // Partida detenida con P.
    protected static final String[] OPCIONES_INICIO = {"JUGAR", "SALIR"}; // Opciones de la portada.
    protected static final String[] OPCIONES_PAUSA = {"CONTINUAR", "REINICIAR", "SALIR"}; // Opciones de la pausa.
    protected static final String[] AYUDA = { // Controles que muestran ambos menús.
        "WASD / FLECHAS: CONDUCIR",
        "ESPACIO: FRENO - C: CAMARA",
        "N: DIA/NOCHE - F: FAROS - M: MAPA",
        "P: PAUSA - R: REINICIAR - ESC: SALIR"
    };
    protected static final String PIE_MENU = "FLECHAS: ELEGIR - ENTER: ACEPTAR"; // Cómo usar el menú.
    private static final float MENU_ANCHO = 440; // Ancho del menú en píxeles de diseño.
    private static final float MENU_ALTO = 330; // Alto del menú en píxeles de diseño.
    protected int estado = INICIO; // El juego arranca en la portada.
    protected int opcion = 0; // Opción seleccionada del menú abierto.

    /** Opciones del menú que está abierto. */
    protected String[] opcionesActuales() {
        return estado == PAUSA ? OPCIONES_PAUSA : OPCIONES_INICIO; // Pausa o portada.
    }

    /** Maneja el menú: flechas (o W/S) mueven la selección, ENTER o ESPACIO eligen, P continúa y ESC sale. */
    private void teclaMenu(int key) {
        int cantidad = opcionesActuales().length; // Opciones disponibles.
        if (key == GLFW_KEY_UP || key == GLFW_KEY_W) { // Subir.
            opcion = (opcion - 1 + cantidad) % cantidad; // Desde la primera pasa a la última.
        }
        if (key == GLFW_KEY_DOWN || key == GLFW_KEY_S) { // Bajar.
            opcion = (opcion + 1) % cantidad; // Desde la última pasa a la primera.
        }
        if (key == GLFW_KEY_ENTER || key == GLFW_KEY_KP_ENTER || key == GLFW_KEY_SPACE) { // Confirmar.
            elegir(opcionesActuales()[opcion]); // Ejecuta la opción seleccionada.
        }
        if (key == GLFW_KEY_P && estado == PAUSA) { // P también quita la pausa.
            estado = JUGANDO; // Vuelve al juego.
        }
        if (key == GLFW_KEY_ESCAPE) { // ESC sale desde cualquier menú.
            salir(); // Cierra la ventana.
        }
    }

    /** Ejecuta una opción del menú. */
    private void elegir(String elegida) {
        if (elegida.equals("JUGAR") || elegida.equals("CONTINUAR")) { // Empezar o seguir.
            estado = JUGANDO; // Cierra el menú.
        } else if (elegida.equals("REINICIAR")) { // Empezar de nuevo.
            reiniciar(); // Auto, entregas, cronómetro y tráfico vuelven al inicio.
            estado = JUGANDO; // Cierra el menú.
        } else if (elegida.equals("SALIR")) { // Terminar.
            salir(); // Cierra la ventana.
        }
    }

    /** Pide cerrar la ventana; sin ventana (en los tests) no hace nada. */
    private void salir() {
        if (ventana != 0) { // Solo si existe una ventana real.
            glfwSetWindowShouldClose(ventana, true); // El ciclo principal termina.
        }
    }

    /** En la portada la cámara aérea de clase1 rodea la ciudad; en el juego, la cámara elegida con C. */
    @Override // Amplía la cámara de clase2.
    protected void configurarCamara() {
        if (estado != INICIO) { // Jugando o en pausa.
            super.configurarCamara(); // Cámara de seguimiento o aérea, según C.
            return; // No cambia nada más.
        }
        boolean anterior = camaraAerea; // Recuerda la cámara elegida por el jugador.
        camaraAerea = true; // clase2 delega en la órbita de clase1.
        try { // Restaura la elección aunque falle.
            super.configurarCamara(); // Cámara que gira alrededor de la ciudad.
        } finally { // Siempre.
            camaraAerea = anterior; // Devuelve la cámara del jugador.
        }
    }

    /** Oscurece la ventana y dibuja el recuadro del menú abierto, centrado. */
    private void dibujarMenu() {
        abrirLienzo(0, 0, ancho, alto, 100, 100); // Toda la ventana.
        try { // Restaura el estado aunque falle.
            rectanguloTranslucido(0, 0, 100, 100, 0, 0, 0, 0.55f); // Oscurece la escena.
        } finally { // Siempre.
            cerrarLienzo(); // Termina el oscurecido.
        }
        float escala = Math.max(0.7f, Math.min(1.6f, alto / 760f)); // Mismo tamaño relativo que el panel.
        escala = Math.min(escala, ancho * 0.9f / MENU_ANCHO); // Nunca más ancho que el 90 % de la ventana.
        escala = Math.min(escala, alto * 0.95f / MENU_ALTO); // Ni más alto que el 95 %.
        int anchoPx = Math.round(MENU_ANCHO * escala); // Ancho real.
        int altoPx = Math.round(MENU_ALTO * escala); // Alto real.
        abrirLienzo((ancho - anchoPx) / 2, (alto - altoPx) / 2, anchoPx, altoPx, MENU_ANCHO, MENU_ALTO); // Centrado.
        try { // Restaura el estado aunque falle.
            rectanguloTranslucido(0, 0, MENU_ANCHO, MENU_ALTO, 0.03f, 0.05f, 0.09f, 0.88f); // Fondo del menú.
            marco(3, 0.6f); // Borde claro.
            if (estado == INICIO) { // Portada.
                textoCentrado("LUCES DE NEON", 266, 6, 1, 0.85f, 0.3f); // Título grande.
                textoCentrado("CONDUCE Y COMPLETA " + DESTINOS.length + " ENTREGAS", 244, 2, 0.75f, 0.8f, 0.9f); // Objetivo.
            } else { // Pausa.
                textoCentrado("PAUSA", 266, 6, 1, 0.85f, 0.3f); // Título grande.
                String progreso = entregas == DESTINOS.length ? "GANASTE EN " + (int) tiempo + " S" // Partida terminada.
                    : "ENTREGAS " + entregas + "/" + DESTINOS.length + " - TIEMPO " + (int) tiempo + " S"; // En curso.
                textoCentrado(progreso, 244, 2, 0.75f, 0.8f, 0.9f); // Estado de la partida.
            }
            dibujarOpciones(); // Opciones con la seleccionada resaltada.
            textoCentrado("CONTROLES", 118, 2, 0.6f, 0.68f, 0.8f); // Título de la ayuda.
            for (int linea = 0; linea < AYUDA.length; linea++) { // Una línea por grupo de teclas.
                textoCentrado(AYUDA[linea], 100 - linea * 16, 2, 0.9f, 0.92f, 0.95f); // Línea de ayuda.
            }
            textoCentrado(PIE_MENU, 18, 2, 0.55f, 0.6f, 0.7f); // Cómo usar el menú.
        } finally { // Siempre.
            cerrarLienzo(); // Vuelve al dibujo normal.
        }
    }

    /** Dibuja las opciones del menú; la seleccionada tiene barra, color amarillo y una flecha. */
    private void dibujarOpciones() {
        String[] opciones = opcionesActuales(); // Opciones del menú abierto.
        for (int indice = 0; indice < opciones.length; indice++) { // Recorre las opciones.
            float py = 206 - indice * 28; // Altura de la opción.
            if (indice == opcion) { // Opción seleccionada.
                rectangulo(110, py - 5, MENU_ANCHO - 220, 30, 0.16f, 0.22f, 0.34f, 20); // Barra resaltada.
                float inicio = (MENU_ANCHO - anchoTexto(opciones[indice], 4)) / 2; // Dónde empieza el texto.
                texto(">", inicio - 22, py, 4, 1, 0.85f, 0.3f); // Flecha delante.
                textoCentrado(opciones[indice], py, 4, 1, 0.85f, 0.3f); // Texto amarillo.
            } else { // Opción normal.
                textoCentrado(opciones[indice], py, 4, 0.85f, 0.88f, 0.92f); // Texto claro.
            }
        }
    }

    // ==================== 10. CARTELES DE SECTORES ====================

    /**
     * Carteles de calle alrededor del Centro: centro X, Z de la calle, 1 si la calle va en Z (el cartel mira al norte
     * y al sur) o 0 si va en X (mira al oeste y al este), y el lado de la acera (-1 o 1) donde va el poste.
     * Van a mitad de cuadra, lejos de farolas y semáforos de las esquinas.
     */
    protected static final float[][] CARTELES = {
        {-10, -20, 1, -1}, // Entrada norte del Centro, poste en la acera del parque.
        {10, 20, 1, 1}, // Entrada sur del Centro.
        {-20, -10, 0, -1}, // Entrada oeste del Centro, poste en la acera del parque.
        {20, 10, 0, 1} // Entrada este del Centro.
    };

    /**
     * Texto de cada cara: la primera mira al norte (o al oeste), la segunda al sur (o al este).
     * Cada cara muestra el sector hacia el que va quien la está leyendo.
     */
    protected static final String[][] TEXTOS_CARTELES = {
        {"CENTRO", "BARRIO NORTE"}, // Quien baja del norte entra al Centro; quien sube va al Barrio Norte.
        {"BARRIO SUR", "CENTRO"}, // Quien baja va al Barrio Sur; quien sube entra al Centro.
        {"CENTRO", "DISTRITO OESTE"}, // Quien viene del oeste entra al Centro; quien va al oeste, al Distrito Oeste.
        {"DISTRITO ESTE", "CENTRO"} // Quien va al este entra al Distrito Este; quien vuelve, al Centro.
    };
    protected static final float SEPARACION_POSTES = 5.4f; // Distancia del centro de la calle al poste: la calzada mide ±5.
    protected static final float LARGO_TABLERO = 2.6f; // Largo del tablero: no llega a las fachadas.
    private static final float ALTO_TABLERO = 1.15f; // Alto del tablero (caben dos líneas).
    private static final float ALTURA_TABLERO = 2.95f; // Centro del tablero: por encima de todos los vehículos.
    protected static final float PIXEL_CARTEL = 0.075f; // Tamaño de cada píxel de la fuente en los carteles.

    /** Separa el nombre en líneas (una por palabra): "BARRIO NORTE" se escribe en dos renglones. */
    protected static String[] lineasCartel(String texto) {
        return texto.split(" "); // CENTRO ocupa una línea; los nombres compuestos, dos.
    }

    /** Dibuja un cartel: poste fino en la acera y tablero compacto con franja de color y nombre en ambas caras. */
    private void dibujarCartel(int indice) {
        float[] cartel = CARTELES[indice]; // Datos del cartel.
        boolean cruzaX = cartel[2] == 1; // El tablero se extiende en X (calle que va en Z).
        float lado = cartel[3]; // Acera elegida.
        float largoX = cruzaX ? 1 : 0; // Dirección del largo del tablero en X.
        float largoZ = cruzaX ? 0 : 1; // Dirección del largo del tablero en Z.
        float posteX = cartel[0] + largoX * lado * SEPARACION_POSTES; // Poste sobre la acera, en X.
        float posteZ = cartel[1] + largoZ * lado * SEPARACION_POSTES; // Poste sobre la acera, en Z.
        float metal = 0.17f; // Gris grafito del poste.
        caja(posteX, 0.36f, posteZ, 0.26f, 0.12f, 0.26f, metal, metal, metal + 0.02f); // Base.
        caja(posteX, 1.95f, posteZ, 0.1f, 3.3f, 0.1f, metal, metal + 0.01f, metal + 0.03f); // Poste fino.
        caja(posteX, 3.63f, posteZ, 0.16f, 0.06f, 0.16f, metal, metal + 0.01f, metal + 0.03f); // Remate.
        float x = posteX - largoX * lado * 0.4f; // El tablero sobresale un poco hacia la calle.
        float z = posteZ - largoZ * lado * 0.4f; // El tablero sobresale un poco hacia la calle.
        float claro = 0.78f; // Borde gris claro.
        caja(x, ALTURA_TABLERO, z, cruzaX ? LARGO_TABLERO + 0.08f : 0.05f, ALTO_TABLERO + 0.08f, cruzaX ? 0.05f : LARGO_TABLERO + 0.08f, claro, claro, claro - 0.02f); // Marco fino.
        if (lucesEncendidas()) { // De noche el cartel brilla.
            entero("uEmision", 1); // Conserva su azul oscuro.
        }
        caja(x, ALTURA_TABLERO, z, cruzaX ? LARGO_TABLERO : 0.08f, ALTO_TABLERO, cruzaX ? 0.08f : LARGO_TABLERO, 0.07f, 0.10f, 0.17f); // Tablero azul noche.
        entero("uEmision", 0); // Restablece la iluminación normal.
        float normalX = cruzaX ? 0 : 1; // La segunda cara mira a +X (este)...
        float normalZ = cruzaX ? 1 : 0; // ...o a +Z (sur).
        caraCartel(TEXTOS_CARTELES[indice][0], x, z, -normalX, -normalZ); // Cara norte u oeste.
        caraCartel(TEXTOS_CARTELES[indice][1], x, z, normalX, normalZ); // Cara sur o este.
    }

    /**
     * Dibuja una cara del tablero cuya normal es (normalX, normalZ): franja superior con el color del sector y su nombre.
     * Quien la lee mira en sentido contrario a la normal, así que su derecha es (normalZ, -normalX).
     */
    private void caraCartel(String texto, float x, float z, float normalX, float normalZ) {
        float[] color = colorSector(indiceSector(texto)); // Color de las fachadas del sector nombrado.
        float frente = 0.05f; // Texto apenas delante del tablero.
        boolean deNoche = lucesEncendidas(); // De noche el cartel se ilumina con suavidad.
        if (deNoche) { // Iluminado.
            entero("uEmision", 1); // Franja y letras brillan.
        }
        float franjaY = ALTURA_TABLERO + ALTO_TABLERO / 2 - 0.09f; // Franja pegada al borde superior.
        float franjaX = normalX != 0 ? 0.02f : LARGO_TABLERO - 0.12f; // Plana sobre la cara.
        float franjaZ = normalZ != 0 ? 0.02f : LARGO_TABLERO - 0.12f; // Plana sobre la cara.
        caja(x + normalX * frente, franjaY, z + normalZ * frente, franjaX, 0.08f, franjaZ, color[0], color[1], color[2]); // Franja de color.
        float tono = deNoche ? 0.92f : 0.96f; // Blanco cálido, un poco más suave de noche.
        String[] lineas = lineasCartel(texto); // Una o dos líneas.
        float altoLinea = 5 * PIXEL_CARTEL; // Alto de una línea de texto.
        float separacion = 0.09f; // Espacio entre líneas.
        float altoTotal = lineas.length * altoLinea + (lineas.length - 1) * separacion; // Alto del bloque de texto.
        float centroTexto = ALTURA_TABLERO - 0.06f; // Un poco por debajo del centro: deja aire bajo la franja.
        for (int linea = 0; linea < lineas.length; linea++) { // Recorre las líneas de arriba abajo.
            float base = centroTexto + altoTotal / 2 - (linea + 1) * altoLinea - linea * separacion; // Borde inferior de la línea.
            escribirEnCara(lineas[linea], x, base, z, normalX, normalZ, frente, tono); // Escribe la línea centrada.
        }
        entero("uEmision", 0); // Restablece la iluminación normal.
    }

    /** Escribe una línea centrada sobre una cara del tablero; base es la altura de su borde inferior. */
    private void escribirEnCara(String texto, float x, float base, float z, float normalX, float normalZ, float frente, float tono) {
        float derechaX = normalZ; // Derecha de quien lee, en X.
        float derechaZ = -normalX; // Derecha de quien lee, en Z.
        float inicio = -anchoTexto(texto, PIXEL_CARTEL) / 2; // Texto centrado en el tablero.
        for (int indice = 0; indice < texto.length(); indice++) { // Recorre los caracteres.
            String glifo = FUENTE.getOrDefault(texto.charAt(indice), FUENTE.get(' ')); // Píxeles del carácter.
            for (int fila = 0; fila < 5; fila++) { // Filas de arriba abajo.
                for (int columna = 0; columna < 3; columna++) { // Columnas de izquierda a derecha.
                    if (glifo.charAt(fila * 3 + columna) != '1') { // Píxel apagado.
                        continue; // Nada que dibujar.
                    }
                    float a = inicio + (indice * 4 + columna) * PIXEL_CARTEL + PIXEL_CARTEL / 2; // Posición a lo largo del tablero.
                    float px = x + derechaX * a + normalX * frente; // Posición X del píxel.
                    float pz = z + derechaZ * a + normalZ * frente; // Posición Z del píxel.
                    float py = base + (4 - fila) * PIXEL_CARTEL + PIXEL_CARTEL / 2; // Altura del píxel.
                    float anchoX = derechaX != 0 ? PIXEL_CARTEL : 0.02f; // Píxel plano sobre la cara.
                    float anchoZ = derechaZ != 0 ? PIXEL_CARTEL : 0.02f; // Píxel plano sobre la cara.
                    caja(px, py, pz, anchoX, PIXEL_CARTEL, anchoZ, tono, tono, tono - 0.04f); // Píxel blanco.
                }
            }
        }
    }

    /** Busca el índice del sector cuyo nombre (en mayúsculas) coincide con el texto del cartel. */
    protected static int indiceSector(String texto) {
        for (int indice = 0; indice < SECTORES.length; indice++) { // Recorre los sectores.
            if (SECTORES[indice].toUpperCase().equals(texto)) { // Coincide el nombre.
                return indice; // Devuelve su índice.
            }
        }
        return 0; // Por defecto, el Centro.
    }

    /** Punto de entrada del proyecto final. */
    public static void main(String[] args) {
        clase4 aplicacion = new clase4(); // Crea la versión con todas las etapas acumuladas.
        aplicacion.run(); // Inicia el ciclo de vida completo de la aplicación.
    }
}
