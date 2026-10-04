package com.graphics; // Reúne la versión final con las otras tres lecciones.

import static org.lwjgl.glfw.GLFW.*; // Permite consultar la tecla que controla el minimapa.
import static org.lwjgl.opengl.GL33.*; // Permite cambiar viewport, recorte y buffers de dibujo.

/**
 * CLASE 4: CIUDAD TERMINADA, ENTREGAS Y MINIMAPA.
 * Orden de lectura: estado, controles, entregas, decoración y segundo pase de dibujo.
 * Hereda clase3, que hereda clase2, que a su vez hereda clase1.
 * Los semáforos son decorativos; el ejemplo no incluye tráfico autónomo.
 */
public class clase4 extends clase3 {

    // ==================== 1. ESTADO DEL JUEGO ====================
    private boolean mostrarMapa = true; // Muestra el minimapa desde el inicio.
    protected int entregas = 0; // Cuenta las entregas completadas; también identifica el siguiente destino.
    protected float tiempo = 0; // Acumula los segundos de la partida hasta completar el recorrido.
    protected float relojCiudad = 0; // Reloj de los semáforos: avanza siempre, también al ganar y tras reiniciar.

    protected static final int FASE_ROJO = 0; // Fase del semáforo: detenerse.
    protected static final int FASE_VERDE = 1; // Fase del semáforo: avanzar.
    protected static final int FASE_AMARILLO = 2; // Fase del semáforo: precaución.
    protected static final float CICLO_SEMAFORO = 12; // Segundos de una vuelta completa rojo → verde → amarillo.
    protected static final float[][] SEMAFOROS = crearSemaforos(); // Cada fila: poste X, Z y sentido X, Z hacia el cruce.

    protected static final int ARBOL_VERDE = 0; // Árbol de hoja verde común.
    protected static final int ARBOL_SAKURA = 1; // Cerezo japonés en flor (#FF69B4).
    protected static final int ARBOL_TAJIBO = 2; // Tajibo blanco en flor (#FDFBF7).
    private static final float[] COLOR_SAKURA = {1.0f, 0.412f, 0.706f}; // #FF69B4 convertido a RGB entre 0 y 1.
    private static final float[] COLOR_TAJIBO = {0.992f, 0.984f, 0.969f}; // #FDFBF7 convertido a RGB entre 0 y 1.
    private static final float CESPED = 0.37f;
    private static final float[] FLOR_BLANCA = {0.98f, 0.97f, 0.94f}; // Flores blancas de los arbustos de la plaza.
    private static final float[][] ILUMINARIAS = {{-2, -2}, {-2, 2}, {2, -2}, {2, 2}}; // Faroles de la plaza respecto a su centro. // Altura de la cara superior del césped de los parques.
    protected static final float[][] DESTINOS = { // Cada fila contiene X y Z de una parada sobre la calle.
        {50, -50}, // Primera entrega: esquina noreste, Barrio Norte.
        {-50, -20}, // Segunda entrega: avenida oeste, Distrito Oeste.
        {0, 10}, // Tercera entrega: frente a la plaza del Centro.
        {30, 50} // Cuarta entrega: avenida sur, Barrio Sur.
    };

    // ==================== 2. CONTROLES Y REINICIO ====================

    /** Añade el interruptor del minimapa a los controles anteriores. */
    @Override // Amplía las teclas definidas por clase3.
    protected void tecla(int key) {
        super.tecla(key); // Conserva salida, cámara, reinicio y luces.
        if (key == GLFW_KEY_M) { // Comprueba si se pulsó la tecla del mapa.
            mostrarMapa = !mostrarMapa; // Alterna entre mostrar y ocultar la vista superior.
        }
    }

    /** Reinicia el vehículo y el progreso de las entregas. */
    @Override // Amplía el reinicio definido en clase2.
    protected void reiniciar() {
        super.reiniciar(); // Restaura posición, velocidad y orientación del vehículo.
        entregas = 0; // Vuelve a seleccionar la primera parada.
        tiempo = 0; // Reinicia el cronómetro de la partida.
    }

    // ==================== 3. REGLAS DE LAS ENTREGAS ====================

    /** Actualiza el vehículo y comprueba si llegó y frenó en el destino activo. */
    @Override // Añade el objetivo del juego al movimiento heredado.
    protected void actualizar(float deltaTime) {
        super.actualizar(deltaTime); // Procesa aceleración, giro, colisiones y título de la ventana.
        relojCiudad += deltaTime; // Los semáforos siguen funcionando aunque la partida haya terminado.
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

    /** Compone el progreso que se añade al título de la ventana. */
    @Override // Amplía los indicadores de día/noche y faros de clase3.
    protected String estadoExtra() {
        String mensaje = super.estadoExtra(); // Recupera los indicadores de iluminación.
        mensaje += " | M: mapa | "; // Muestra la tecla que alterna el minimapa.
        if (entregas == DESTINOS.length) { // Selecciona el texto de victoria al completar todas las paradas.
            mensaje += "GANASTE en " + (int) tiempo + " s! R: jugar otra vez"; // Muestra tiempo final y opción de reinicio.
        } else { // Durante el recorrido muestra progreso e instrucciones.
            mensaje += "Entregas " + entregas + "/" + DESTINOS.length; // Indica cuántas paradas se completaron.
            mensaje += " | Frena en la marca dorada | " + (int) tiempo + " s"; // Explica la condición de entrega y el tiempo.
        }
        return mensaje; // Entrega el texto a actualizarTitulo() de clase2.
    }

    // ==================== 4. ESCENA FINAL Y DESTINO ====================

    /** Añade decoración y señal del destino a la escena iluminada. */
    @Override // Amplía el dibujo acumulado de las tres etapas anteriores.
    protected void escena() {
        super.escena(); // Dibuja la ciudad, el auto y las farolas.
        if (!vistaMapa) { // Los detalles pequeños solo son necesarios en la vista principal.
            decorarCiudad(); // Añade árboles, bancos, ventanas y señalización urbana.
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
    }

    // ---------- Parques: jardín japonés ----------

    /** Elige la especie de un árbol; el patrón mezcla las tres especies en cada parque y siempre da lo mismo. */
    protected static int especieArbol(int fila, int columna, int indice) {
        return (fila + columna + indice) % 3; // 0 verde común, 1 sakura, 2 tajibo blanco.
    }

    /** Dibuja cuatro árboles, un sendero de piedras, una linterna de piedra y dos bancos. */
    private void dibujarParque(int fila, int columna, float x, float z) {
        if (fila == PLAZA && columna == PLAZA) { // El parque del centro de la ciudad es especial.
            dibujarPlazaCentral(x, z); // Fuente, iluminarias y bancos alrededor.
            return; // No lleva la decoración de los demás parques.
        }
        float[] esquinas = {-2.8f, 2.8f}; // Desplazamientos de los árboles respecto al centro del parque.
        int indice = 0; // Numera los árboles para elegir su especie.
        for (float desplazamientoX : esquinas) { // Lado izquierdo o derecho del parque.
            for (float desplazamientoZ : esquinas) { // Lado norte o sur del parque.
                dibujarArbol(x + desplazamientoX, z + desplazamientoZ, especieArbol(fila, columna, indice)); // Planta un árbol.
                indice++; // Pasa al siguiente árbol.
            }
        }
        dibujarSendero(x, z); // Losas de piedra que cruzan el parque de norte a sur.
        dibujarToro(x, z); // Linterna de piedra en el centro.
        dibujarBanco(x - 1.6f, z, false); // Banco al oeste de la linterna.
        dibujarBanco(x + 1.6f, z, false); // Banco al este de la linterna.
        dibujarBasurero(x - 1.6f, z + 1.35f); // Basurero junto al banco oeste.
        dibujarBasurero(x + 1.6f, z - 1.35f); // Basurero junto al banco este.
        dibujarBebedero(x + 3.8f, z); // Bebedero en el borde este del parque.
        dibujarArbusto(x - 1.2f, z - 3.7f, COLOR_SAKURA); // Arbustos a ambos lados de la entrada norte del sendero.
        dibujarArbusto(x + 1.2f, z - 3.7f, null); // El segundo, sin flores, para variar.
        dibujarArbusto(x - 1.2f, z + 3.7f, null); // Arbustos a ambos lados de la entrada sur.
        dibujarArbusto(x + 1.2f, z + 3.7f, COLOR_SAKURA); // Con flores rosadas.
        dibujarArbusto(x - 3.8f, z, COLOR_SAKURA); // Arbusto en el borde oeste, frente al bebedero.
    }

    // ---------- Plaza central ----------

    /** Plaza del centro: piso de piedra, fuente, iluminarias, cuatro bancos, árboles en flor y arbustos floridos. */
    private void dibujarPlazaCentral(float x, float z) {
        float diagonal = (float) (Math.PI / 4); // Giro que convierte dos cuadrados en un octágono.
        caja(x, CESPED + 0.015f, z, 5.4f, 0.03f, 5.4f, 0.80f, 0.77f, 0.70f); // Piso de piedra clara.
        cajaGirada(x, CESPED + 0.015f, z, 5.4f, 0.03f, 5.4f, 0.80f, 0.77f, 0.70f, diagonal); // Piso girado: forma octogonal.
        dibujarFuente(x, z); // Fuente de agua en el centro.
        dibujarArbol(x - 2.8f, z - 2.8f, ARBOL_SAKURA); // Sakuras y tajibos alternados en diagonal.
        dibujarArbol(x + 2.8f, z + 2.8f, ARBOL_SAKURA); // Segundo sakura.
        dibujarArbol(x - 2.8f, z + 2.8f, ARBOL_TAJIBO); // Primer tajibo blanco.
        dibujarArbol(x + 2.8f, z - 2.8f, ARBOL_TAJIBO); // Segundo tajibo blanco.
        dibujarBanco(x - 3.3f, z, false); // Banco oeste, mirando a la fuente.
        dibujarBanco(x + 3.3f, z, false); // Banco este.
        dibujarBanco(x, z - 3.3f, true); // Banco norte.
        dibujarBanco(x, z + 3.3f, true); // Banco sur.
        for (float[] farol : ILUMINARIAS) { // Faroles en las cuatro diagonales.
            dibujarIluminaria(x + farol[0], z + farol[1]); // Construye cada farol.
        }
        for (int lado = -1; lado <= 1; lado += 2) { // Recorre ambos lados de cada eje.
            dibujarArbusto(x - 1.7f, z + lado * 4.0f, COLOR_SAKURA); // Arbustos junto a los bancos norte y sur.
            dibujarArbusto(x + 1.7f, z + lado * 4.0f, FLOR_BLANCA); // Alterna flores rosadas y blancas.
            dibujarArbusto(x + lado * 4.0f, z - 1.7f, FLOR_BLANCA); // Arbustos junto a los bancos oeste y este.
            dibujarArbusto(x + lado * 4.0f, z + 1.7f, COLOR_SAKURA); // Con flores rosadas.
        }
        dibujarBasurero(x + 3.2f, z - 1.3f); // Basurero junto al banco este.
        dibujarBasurero(x - 3.2f, z + 1.3f); // Basurero junto al banco oeste.
    }

    /** Construye una fuente de tres niveles con agua animada; de noche el agua se ilumina. */
    private void dibujarFuente(float x, float z) {
        float diagonal = (float) (Math.PI / 4); // Giro para las piletas octogonales.
        float piedra = 0.86f; // Piedra clara de la fuente.
        caja(x, CESPED + 0.25f, z, 3.4f, 0.5f, 3.4f, piedra, piedra - 0.02f, piedra - 0.07f); // Pileta exterior.
        cajaGirada(x, CESPED + 0.25f, z, 3.4f, 0.5f, 3.4f, piedra, piedra - 0.02f, piedra - 0.07f, diagonal); // Pileta exterior girada.
        caja(x, CESPED + 0.95f, z, 0.45f, 0.9f, 0.45f, piedra - 0.05f, piedra - 0.07f, piedra - 0.12f); // Columna central.
        caja(x, CESPED + 1.45f, z, 1.5f, 0.18f, 1.5f, piedra, piedra - 0.02f, piedra - 0.07f); // Pileta intermedia.
        cajaGirada(x, CESPED + 1.45f, z, 1.5f, 0.18f, 1.5f, piedra, piedra - 0.02f, piedra - 0.07f, diagonal); // Pileta intermedia girada.
        caja(x, CESPED + 1.79f, z, 0.22f, 0.5f, 0.22f, piedra - 0.05f, piedra - 0.07f, piedra - 0.12f); // Columna superior.
        caja(x, CESPED + 2.1f, z, 0.7f, 0.12f, 0.7f, piedra, piedra - 0.02f, piedra - 0.07f); // Cuenco alto.
        cajaGirada(x, CESPED + 2.1f, z, 0.7f, 0.12f, 0.7f, piedra, piedra - 0.02f, piedra - 0.07f, diagonal); // Cuenco alto girado.

        float[] agua = {0.25f, 0.58f, 0.88f}; // Azul del agua de día.
        float[] chorro = {0.70f, 0.88f, 1.0f}; // Agua en movimiento, más clara.
        if (lucesEncendidas()) { // De noche focos sumergidos iluminan el agua.
            entero("uEmision", 1); // El agua brilla por sí misma.
            agua = new float[] {0.30f, 0.75f, 1.0f}; // Celeste luminoso.
            chorro = new float[] {0.80f, 0.95f, 1.0f}; // Chorros casi blancos.
        }
        caja(x, CESPED + 0.51f, z, 3.0f, 0.02f, 3.0f, agua[0], agua[1], agua[2]); // Agua de la pileta exterior.
        cajaGirada(x, CESPED + 0.51f, z, 3.0f, 0.02f, 3.0f, agua[0], agua[1], agua[2], diagonal); // Agua exterior girada.
        caja(x, CESPED + 1.55f, z, 1.3f, 0.02f, 1.3f, agua[0], agua[1], agua[2]); // Agua de la pileta intermedia.
        cajaGirada(x, CESPED + 1.55f, z, 1.3f, 0.02f, 1.3f, agua[0], agua[1], agua[2], diagonal); // Agua intermedia girada.
        caja(x, CESPED + 2.17f, z, 0.56f, 0.02f, 0.56f, agua[0], agua[1], agua[2]); // Agua del cuenco alto.

        float altura = 0.55f + 0.15f * (float) Math.sin(relojCiudad * 3); // El chorro central sube y baja.
        caja(x, CESPED + 2.18f + altura / 2, z, 0.08f, altura, 0.08f, chorro[0], chorro[1], chorro[2]); // Chorro central.
        cajaGirada(x, CESPED + 2.2f + altura, z, 0.2f, 0.1f, 0.2f, chorro[0], chorro[1], chorro[2], relojCiudad * 2); // Remate del chorro, girando.
        float ondas = 0.25f + 0.05f * (float) Math.sin(relojCiudad * 4); // Las salpicaduras cambian de tamaño.
        for (int indice = 0; indice < 4; indice++) { // Cuatro cascadas desde la pileta intermedia.
            float angulo = indice * (float) (Math.PI / 2); // Norte, este, sur y oeste.
            float cascadaX = x + (float) Math.sin(angulo) * 1.1f; // Justo afuera de la pileta intermedia.
            float cascadaZ = z + (float) Math.cos(angulo) * 1.1f; // Justo afuera de la pileta intermedia.
            cajaGirada(cascadaX, CESPED + 0.98f, cascadaZ, 0.22f, 0.92f, 0.05f, chorro[0], chorro[1], chorro[2], angulo); // Lámina de agua cayendo.
            cajaGirada(cascadaX, CESPED + 0.53f, cascadaZ, ondas, 0.02f, ondas, chorro[0], chorro[1], chorro[2], relojCiudad + angulo); // Salpicadura.
        }
        entero("uEmision", 0); // El resto de la plaza recibe luz normal.
    }

    /** Construye un farol de jardín bajo (por debajo de las copas) que se enciende de noche. */
    private void dibujarIluminaria(float x, float z) {
        float metal = 0.13f; // Metal oscuro.
        caja(x, CESPED + 0.075f, z, 0.3f, 0.15f, 0.3f, metal, metal + 0.01f, metal + 0.03f); // Base.
        caja(x, CESPED + 0.76f, z, 0.08f, 1.22f, 0.08f, metal + 0.04f, metal + 0.05f, metal + 0.07f); // Poste.
        float[] luz = {0.60f, 0.62f, 0.60f}; // Vidrio apagado de día.
        if (lucesEncendidas()) { // De noche el farol alumbra.
            entero("uEmision", 1); // El farol es una fuente de luz.
            luz = new float[] {1.0f, 0.85f, 0.55f}; // Luz cálida.
        }
        caja(x, CESPED + 1.55f, z, 0.3f, 0.36f, 0.3f, luz[0], luz[1], luz[2]); // Farol.
        entero("uEmision", 0); // Tapa y remate reciben luz normal.
        cajaGirada(x, CESPED + 1.76f, z, 0.42f, 0.06f, 0.42f, metal, metal + 0.01f, metal + 0.03f, (float) (Math.PI / 4)); // Tapa.
        caja(x, CESPED + 1.83f, z, 0.08f, 0.08f, 0.08f, metal, metal + 0.01f, metal + 0.03f); // Remate.
    }

    /** Suma los halos nocturnos de la plaza central a los de farolas y auto. */
    @Override // Usa el gancho que clase3 llama con la mezcla aditiva activada.
    protected void halosAdicionales() {
        if (factorNoche <= 0) { // De día no hay halos de la plaza.
            return; // Termina sin dibujar.
        }
        float plazaX = centro(PLAZA); // Centro X de la plaza.
        float plazaZ = centro(PLAZA); // Centro Z de la plaza.
        float fuerza = factorNoche * 0.6f; // Crece con la transición a la noche.
        for (float[] farol : ILUMINARIAS) { // Un halo cálido por farol.
            caja(plazaX + farol[0], CESPED + 1.55f, plazaZ + farol[1], 1.5f, 1.5f, 1.5f, fuerza, fuerza * 0.8f, fuerza * 0.5f); // Halo del farol.
        }
        caja(plazaX, CESPED + 1.3f, plazaZ, 5, 5, 5, fuerza * 0.12f, fuerza * 0.35f, fuerza * 0.55f); // Resplandor celeste de la fuente.
    }

    /** Construye un arbusto redondeado; con un color de flor (o null sin flores) parece una azalea. */
    private void dibujarArbusto(float x, float z, float[] flor) {
        caja(x, CESPED + 0.3f, z, 0.9f, 0.6f, 0.9f, 0.13f, 0.40f, 0.17f); // Cuerpo del arbusto.
        cajaGirada(x, CESPED + 0.42f, z, 0.75f, 0.6f, 0.75f, 0.17f, 0.48f, 0.21f, (float) (Math.PI / 4)); // Capa girada: silueta redonda.
        if (flor != null) { // Azalea: florecitas sobre el follaje.
            caja(x - 0.22f, CESPED + 0.74f, z + 0.1f, 0.14f, 0.06f, 0.14f, flor[0], flor[1], flor[2]); // Flor izquierda.
            caja(x + 0.2f, CESPED + 0.72f, z - 0.18f, 0.14f, 0.06f, 0.14f, flor[0], flor[1], flor[2]); // Flor derecha.
            caja(x + 0.05f, CESPED + 0.75f, z + 0.25f, 0.14f, 0.06f, 0.14f, flor[0], flor[1], flor[2]); // Flor trasera.
        }
    }

    /** Construye un basurero octogonal verde oscuro con tapa gris. */
    private void dibujarBasurero(float x, float z) {
        caja(x, CESPED + 0.32f, z, 0.4f, 0.64f, 0.4f, 0.12f, 0.26f, 0.18f); // Cuerpo.
        cajaGirada(x, CESPED + 0.32f, z, 0.4f, 0.64f, 0.4f, 0.12f, 0.26f, 0.18f, (float) (Math.PI / 4)); // Cuerpo girado: forma octogonal.
        caja(x, CESPED + 0.67f, z, 0.46f, 0.06f, 0.46f, 0.45f, 0.46f, 0.48f); // Tapa gris.
        caja(x, CESPED + 0.6f, z, 0.2f, 0.03f, 0.48f, 0.05f, 0.05f, 0.05f); // Ranura oscura bajo la tapa.
    }

    /** Construye un bebedero: pedestal de piedra, cuenca, agua y caño metálico. */
    private void dibujarBebedero(float x, float z) {
        caja(x, CESPED + 0.04f, z, 0.6f, 0.08f, 0.6f, 0.50f, 0.50f, 0.48f); // Base de piedra.
        caja(x, CESPED + 0.45f, z, 0.32f, 0.8f, 0.32f, 0.60f, 0.60f, 0.58f); // Pedestal.
        caja(x, CESPED + 0.9f, z, 0.62f, 0.12f, 0.48f, 0.78f, 0.79f, 0.80f); // Cuenca gris claro.
        caja(x, CESPED + 0.965f, z, 0.48f, 0.02f, 0.34f, 0.35f, 0.70f, 0.95f); // Agua celeste.
        caja(x - 0.12f, CESPED + 1.05f, z, 0.06f, 0.14f, 0.06f, 0.70f, 0.72f, 0.75f); // Caño metálico.
    }

    /** Construye un árbol con cajas; cada especie tiene su tronco, su copa y, si florece, pétalos en el suelo. */
    private void dibujarArbol(float x, float z, int especie) {
        float diagonal = (float) (Math.PI / 4); // Girar capas 45° redondea la silueta de la copa.
        if (especie == ARBOL_SAKURA) { // Cerezo: tronco rojizo y copa ancha y plana en capas rosadas.
            float[] flor = COLOR_SAKURA; // Rosa del sakura.
            cajaGirada(x, CESPED + 0.01f, z, 2.4f, 0.02f, 2.4f, flor[0], flor[1], flor[2], diagonal); // Pétalos caídos (giro).
            caja(x, CESPED + 0.01f, z, 2.4f, 0.02f, 2.4f, flor[0], flor[1], flor[2]); // Pétalos caídos: con la otra forman un octágono.
            caja(x, CESPED + 1.0f, z, 0.3f, 2.0f, 0.3f, 0.30f, 0.17f, 0.15f); // Tronco oscuro rojizo.
            caja(x, 2.75f, z, 2.6f, 0.8f, 2.6f, flor[0] * 0.92f, flor[1] * 0.92f, flor[2] * 0.92f); // Capa baja, algo en sombra.
            cajaGirada(x, 3.45f, z, 2.4f, 0.7f, 2.4f, flor[0], flor[1], flor[2], diagonal); // Capa media girada.
            caja(x, 4.05f, z, 1.4f, 0.55f, 1.4f, 1.0f, 0.62f, 0.82f); // Copa superior más clara.
        } else if (especie == ARBOL_TAJIBO) { // Tajibo blanco: tronco claro y copa redondeada blanca.
            float[] flor = COLOR_TAJIBO; // Blanco del tajibo.
            cajaGirada(x, CESPED + 0.01f, z, 2.2f, 0.02f, 2.2f, flor[0], flor[1], flor[2], diagonal); // Pétalos caídos (giro).
            caja(x, CESPED + 0.01f, z, 2.2f, 0.02f, 2.2f, flor[0], flor[1], flor[2]); // Pétalos caídos.
            caja(x, CESPED + 1.15f, z, 0.3f, 2.3f, 0.3f, 0.52f, 0.47f, 0.42f); // Tronco gris claro.
            cajaGirada(x, 2.95f, z, 2.2f, 0.8f, 2.2f, 0.90f, 0.89f, 0.87f, diagonal); // Capa baja en sombra.
            caja(x, 3.6f, z, 2.5f, 0.8f, 2.5f, flor[0], flor[1], flor[2]); // Capa media, la más ancha.
            cajaGirada(x, 4.3f, z, 1.6f, 0.6f, 1.6f, flor[0], flor[1], flor[2], diagonal); // Remate redondeado.
        } else { // Árbol verde común: copa en dos niveles.
            caja(x, CESPED + 1.1f, z, 0.35f, 2.2f, 0.35f, 0.38f, 0.24f, 0.14f); // Tronco marrón.
            caja(x, 2.9f, z, 2.4f, 1.6f, 2.4f, 0.10f, 0.36f, 0.18f); // Copa baja verde oscuro.
            cajaGirada(x, 4.1f, z, 1.6f, 1.3f, 1.6f, 0.18f, 0.50f, 0.25f, diagonal); // Copa alta más clara.
        }
    }

    /** Coloca losas planas espaciadas (tobi-ishi) a lo largo del eje norte-sur del parque. */
    private void dibujarSendero(float x, float z) {
        float[] distancias = {1.2f, 2.1f, 3.0f, 3.9f}; // Distancia de cada losa al centro.
        for (int lado = -1; lado <= 1; lado += 2) { // Mitad norte y mitad sur.
            for (int indice = 0; indice < distancias.length; indice++) { // Recorre las losas de esa mitad.
                float corrimiento = indice % 2 == 0 ? 0.12f : -0.12f; // Alterna un poco a cada lado: aspecto natural.
                caja(x + corrimiento, CESPED + 0.02f, z + lado * distancias[indice], 0.75f, 0.05f, 0.6f, 0.64f, 0.63f, 0.60f); // Losa.
            }
        }
    }

    /** Construye una linterna de piedra japonesa (tōrō) que se enciende de noche. */
    private void dibujarToro(float x, float z) {
        float piedra = 0.56f; // Gris de la piedra.
        caja(x, CESPED + 0.1f, z, 0.9f, 0.2f, 0.9f, piedra - 0.06f, piedra - 0.06f, piedra - 0.08f); // Base.
        caja(x, CESPED + 0.65f, z, 0.3f, 0.9f, 0.3f, piedra, piedra, piedra - 0.03f); // Columna.
        caja(x, CESPED + 1.16f, z, 0.7f, 0.12f, 0.7f, piedra, piedra, piedra - 0.03f); // Plataforma de la lámpara.
        float[] luz = {piedra - 0.1f, piedra - 0.1f, piedra - 0.12f}; // De día la caja de luz es de piedra oscura.
        if (lucesEncendidas()) { // De noche la vela interior brilla.
            entero("uEmision", 1); // La caja de luz es una fuente de luz.
            luz = new float[] {1.0f, 0.78f, 0.45f}; // Luz cálida.
        }
        caja(x, CESPED + 1.45f, z, 0.46f, 0.45f, 0.46f, luz[0], luz[1], luz[2]); // Caja de luz.
        entero("uEmision", 0); // El resto de la linterna recibe luz normal.
        caja(x, CESPED + 1.74f, z, 1.0f, 0.12f, 1.0f, piedra - 0.08f, piedra - 0.08f, piedra - 0.1f); // Tejadillo ancho.
        cajaGirada(x, CESPED + 1.86f, z, 0.6f, 0.14f, 0.6f, piedra - 0.08f, piedra - 0.08f, piedra - 0.1f, (float) (Math.PI / 4)); // Segunda capa del tejado.
        caja(x, CESPED + 2.0f, z, 0.16f, 0.16f, 0.16f, piedra, piedra, piedra - 0.03f); // Remate superior.
    }

    /** Construye un banco japonés bajo, sin respaldo: tres listones de madera sobre dos bloques de piedra. */
    private void dibujarBanco(float x, float z, boolean largoEnX) {
        for (int lado = -1; lado <= 1; lado += 2) { // Un bloque en cada extremo.
            if (largoEnX) { // Banco orientado de oeste a este.
                caja(x + lado * 0.6f, CESPED + 0.18f, z, 0.3f, 0.36f, 0.55f, 0.34f, 0.35f, 0.36f); // Apoyo de piedra oscura.
            } else { // Banco orientado de norte a sur.
                caja(x, CESPED + 0.18f, z + lado * 0.6f, 0.55f, 0.36f, 0.3f, 0.34f, 0.35f, 0.36f); // Apoyo de piedra oscura.
            }
        }
        for (int liston = -1; liston <= 1; liston++) { // Tres listones con una pequeña separación.
            if (largoEnX) { // Listones a lo largo de X.
                caja(x, CESPED + 0.39f, z + liston * 0.2f, 1.8f, 0.06f, 0.16f, 0.78f, 0.60f, 0.40f); // Listón de madera clara.
            } else { // Listones a lo largo de Z.
                caja(x + liston * 0.2f, CESPED + 0.39f, z, 0.16f, 0.06f, 1.8f, 0.78f, 0.60f, 0.40f); // Listón de madera clara.
            }
        }
    }

    // ---------- Edificios ----------

    /** Distribuye ventanas por pisos: vidrio oscuro de día; de noche unas encendidas y otras apagadas. */
    private void dibujarVentanas(int fila, int columna, float x, float z, float altura) {
        boolean deNoche = lucesEncendidas(); // Las habitaciones solo se iluminan de noche.
        int piso = 0; // Numera los pisos para variar qué ventanas se encienden.
        for (float y = 1.7f; y + 0.45f < altura; y += 2) { // Recorre los pisos sin invadir la cornisa.
            for (int ventana = 0; ventana < 3; ventana++) { // Tres ventanas por fachada.
                float desplazamiento = (ventana - 1) * 2; // -2, 0 o 2 respecto al centro de la fachada.
                for (int fachada = 0; fachada < 4; fachada++) { // Norte, sur, oeste y este.
                    int patron = fila * 7 + columna * 5 + piso * 3 + ventana * 11 + fachada * 13; // Número reproducible.
                    boolean encendida = deNoche && patron % 4 != 0; // Tres de cada cuatro ventanas encendidas.
                    float r = 0.24f; // Vidrio azulado oscuro por defecto.
                    float g = 0.32f; // Componente verde del vidrio.
                    float b = 0.42f; // Componente azul del vidrio.
                    entero("uEmision", 0); // Una ventana apagada recibe la luz del entorno.
                    if (encendida) { // Habitación iluminada.
                        entero("uEmision", 1); // Brilla sin depender de las farolas.
                        r = 0.98f; // Luz interior cálida.
                        g = 0.80f; // Componente verde de la luz cálida.
                        b = 0.45f; // Componente azul de la luz cálida.
                    }
                    if (fachada == 0) { // Fachada norte.
                        caja(x + desplazamiento, y, z - 3.51f, 0.8f, 0.9f, 0.04f, r, g, b); // Ventana norte.
                    } else if (fachada == 1) { // Fachada sur.
                        caja(x + desplazamiento, y, z + 3.51f, 0.8f, 0.9f, 0.04f, r, g, b); // Ventana sur.
                    } else if (fachada == 2) { // Fachada oeste.
                        caja(x - 3.51f, y, z + desplazamiento, 0.04f, 0.9f, 0.8f, r, g, b); // Ventana oeste.
                    } else { // Fachada este.
                        caja(x + 3.51f, y, z + desplazamiento, 0.04f, 0.9f, 0.8f, r, g, b); // Ventana este.
                    }
                }
            }
            piso++; // Pasa al piso siguiente.
        }
        entero("uEmision", 0); // Restablece la iluminación normal de los demás elementos.
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

    /** Construye un poste con dos cabezales: uno para el tráfico en Z y otro, con la fase opuesta, para el tráfico en X. */
    private void dibujarSemaforo(float[] esquina) {
        float x = esquina[0]; // Poste X sobre la acera.
        float z = esquina[1]; // Poste Z sobre la acera.
        float haciaX = esquina[2]; // Sentido X hacia el cruce.
        float haciaZ = esquina[3]; // Sentido Z hacia el cruce.
        caja(x, 0.45f, z, 0.36f, 0.3f, 0.36f, 0.16f, 0.17f, 0.19f); // Base sobre la acera.
        caja(x, 2.1f, z, 0.16f, 3.6f, 0.16f, 0.20f, 0.22f, 0.25f); // Poste.
        caja(x + haciaX * 0.2f, 3.6f, z, 0.4f, 0.1f, 0.1f, 0.20f, 0.22f, 0.25f); // Brazo corto hacia la calle vertical.
        caja(x, 3.6f, z + haciaZ * 0.2f, 0.1f, 0.1f, 0.4f, 0.20f, 0.22f, 0.25f); // Brazo corto hacia la calle horizontal.
        dibujarCabezal(x + haciaX * 0.45f, z, true, faseSemaforo(relojCiudad)); // Controla a los autos que avanzan en Z.
        dibujarCabezal(x, z + haciaZ * 0.45f, false, faseSemaforo(relojCiudad + 6)); // Mientras uno está en rojo, el otro avanza.
    }

    /** Dibuja la carcasa y las tres luces (rojo arriba, amarillo, verde abajo) en las dos caras del cabezal. */
    private void dibujarCabezal(float x, float z, boolean miraZ, int fase) {
        float y = 3.4f; // Altura del centro del cabezal.
        float anchoX = miraZ ? 0.42f : 0.36f; // La cara con luces es la más ancha.
        float anchoZ = miraZ ? 0.36f : 0.42f; // Profundidad de la carcasa.
        caja(x, y, z, anchoX, 1.15f, anchoZ, 0.07f, 0.08f, 0.10f); // Carcasa negra.
        int[] fases = {FASE_ROJO, FASE_AMARILLO, FASE_VERDE}; // Fase de cada luz, de arriba a abajo.
        float[][] colores = {{1, 0.12f, 0.08f}, {1, 0.70f, 0.05f}, {0.15f, 1, 0.35f}}; // Rojo, amarillo y verde.
        for (int indice = 0; indice < 3; indice++) { // Recorre las tres luces.
            boolean encendida = fase == fases[indice]; // Solo brilla la luz de la fase actual.
            float brillo = 0.18f; // Una luz apagada conserva un color muy tenue.
            entero("uEmision", 0); // Una luz apagada recibe la iluminación normal.
            if (encendida) { // Luz de la fase activa.
                brillo = 1; // Color completo.
                entero("uEmision", 1); // Se ve encendida también de día.
            }
            float r = colores[indice][0] * brillo; // Rojo de esta luz.
            float g = colores[indice][1] * brillo; // Verde de esta luz.
            float b = colores[indice][2] * brillo; // Azul de esta luz.
            float altura = y + 0.36f - indice * 0.36f; // Separa verticalmente las tres luces.
            for (int lado = -1; lado <= 1; lado += 2) { // Las dos caras del cabezal, para ambos sentidos.
                if (miraZ) { // Luces visibles desde el norte y el sur.
                    caja(x, altura, z + lado * 0.19f, 0.26f, 0.26f, 0.04f, r, g, b); // Luz en la cara Z.
                } else { // Luces visibles desde el oeste y el este.
                    caja(x + lado * 0.19f, altura, z, 0.04f, 0.26f, 0.26f, r, g, b); // Luz en la cara X.
                }
            }
        }
        entero("uEmision", 0); // Evita que el siguiente objeto herede la emisión del semáforo.
    }

    // ==================== 6. MINIMAPA: SEGUNDO PASE DE DIBUJO ====================

    /** Dibuja la escena principal y después la misma ciudad desde arriba, en un recuadro. */
    @Override // Amplía el cuadro completo definido en clase1.
    protected void dibujarFrame() {
        super.dibujarFrame(); // Dibuja primero la vista normal de la ciudad.
        if (!mostrarMapa) { // Comprueba si el usuario ocultó el minimapa con M.
            return; // Conserva únicamente la imagen principal.
        }
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
        entero("uMapa", 1); // Selecciona la proyección ortográfica del shader de clase1.
        decimal("uEscalaMapa", LIMITE + 6); // Deja un margen arriba para la marca del norte.
        vistaMapa = true; // Indica a escena() que omita decoración pequeña y baliza flotante.
        try { // Asegura que el estado de dibujo se restaure incluso si el segundo pase falla.
            escena(); // Dibuja otra vez la misma ciudad, ahora vista desde arriba.
            dibujarIndicadorAuto(); // Resalta la posición y el frente del jugador en el mapa.
            dibujarNorte(); // Escribe una N en el borde superior: el norte (-Z) queda arriba.
        } finally { // El siguiente cuadro debe volver a la configuración de pantalla completa.
            vistaMapa = false; // Reactiva los detalles de la escena principal.
            entero("uMapa", 0); // Recupera la proyección en perspectiva.
            glDisable(GL_SCISSOR_TEST); // Permite que la próxima limpieza abarque toda la pantalla.
            glViewport(0, 0, ancho, alto); // Recupera el área de dibujo de la ventana completa.
        }
        if (glGetError() != GL_NO_ERROR) { // Comprueba que el pase del mapa no haya generado errores OpenGL.
            throw new IllegalStateException("Error OpenGL en minimapa"); // Expone el error en la consola.
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

    /** Punto de entrada del proyecto final. */
    public static void main(String[] args) {
        clase4 aplicacion = new clase4(); // Crea la versión con todas las etapas acumuladas.
        aplicacion.run(); // Inicia el ciclo de vida completo de la aplicación.
    }
}
