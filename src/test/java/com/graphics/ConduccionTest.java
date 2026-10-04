package com.graphics; // Permite probar los métodos protegidos del juego desde el mismo paquete.

import java.util.ArrayDeque; // Cola usada para recorrer la red de calles.
import junit.framework.TestCase; // Proporciona las comprobaciones de JUnit usadas por Maven.

/** Comprueba las reglas de la ciudad y de la conducción sin abrir una ventana OpenGL. */
public class ConduccionTest extends TestCase {

    /** Comprueba el tamaño mínimo del mapa y la cantidad de edificios y parques. */
    public void testMapaAmpliado() {
        assertTrue(clase1.MAPA.length >= 11); // Exige al menos 11 filas.
        int edificios = 0; // Cuenta las celdas con edificio.
        int parques = 0; // Cuenta las celdas con parque.
        for (int[] fila : clase1.MAPA) { // Recorre cada fila del mapa.
            assertEquals(clase1.MAPA.length, fila.length); // Exige un mapa cuadrado.
            for (int celda : fila) { // Recorre cada celda de la fila.
                if (celda == 1) { // Detecta un edificio.
                    edificios++; // Suma un edificio.
                }
                if (celda == 2) { // Detecta un parque.
                    parques++; // Suma un parque.
                }
            }
        }
        assertTrue(edificios >= 12); // Exige al menos 12 manzanas con edificios.
        assertTrue(parques >= 4); // Exige al menos 4 parques.
        assertEquals(clase1.LIMITE * 2, clase1.MAPA.length * clase1.CELDA, 0f); // El suelo cubre todas las celdas.
    }

    /** Comprueba que los edificios varían en altura y color. */
    public void testVariacionEdificios() {
        float alturaMinima = Float.MAX_VALUE; // Guarda la menor altura encontrada.
        float alturaMaxima = 0; // Guarda la mayor altura encontrada.
        java.util.Set<String> colores = new java.util.HashSet<>(); // Guarda los colores distintos.
        for (int fila = 0; fila < clase1.MAPA.length; fila++) { // Recorre las filas.
            for (int columna = 0; columna < clase1.MAPA.length; columna++) { // Recorre las columnas.
                if (clase1.MAPA[fila][columna] != 1) { // Solo interesan los edificios.
                    continue; // Omite calles y parques.
                }
                float altura = clase1.alturaEdificio(fila, columna); // Obtiene la altura del edificio.
                alturaMinima = Math.min(alturaMinima, altura); // Actualiza el mínimo.
                alturaMaxima = Math.max(alturaMaxima, altura); // Actualiza el máximo.
                float[] tono = clase1.colorEdificio(fila, columna); // Obtiene el color del edificio.
                colores.add(tono[0] + "," + tono[1] + "," + tono[2]); // Registra el color como texto.
            }
        }
        assertTrue(alturaMaxima - alturaMinima >= 8); // Exige diferencias de altura visibles.
        assertTrue(alturaMaxima < 24); // Los edificios quedan por debajo de las marcas del minimapa.
        assertTrue(colores.size() >= 5); // Exige varios colores distintos.
    }

    /** Comprueba que todas las calles forman una sola red conectada. */
    public void testCallesConectadas() {
        int lado = clase1.MAPA.length; // Obtiene el tamaño del mapa.
        boolean[][] visitada = new boolean[lado][lado]; // Marca las calles alcanzadas.
        ArrayDeque<int[]> pendientes = new ArrayDeque<>(); // Guarda las celdas por explorar.
        pendientes.add(new int[] {0, 0}); // Empieza en la esquina noroeste, que es calle.
        visitada[0][0] = true; // Marca la celda inicial.
        int[][] pasos = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}; // Vecinos en las cuatro direcciones.
        while (!pendientes.isEmpty()) { // Explora mientras queden celdas.
            int[] actual = pendientes.poll(); // Toma la siguiente celda.
            for (int[] paso : pasos) { // Revisa cada vecino.
                int fila = actual[0] + paso[0]; // Calcula la fila vecina.
                int columna = actual[1] + paso[1]; // Calcula la columna vecina.
                boolean dentro = fila >= 0 && fila < lado && columna >= 0 && columna < lado; // Comprueba los límites.
                if (dentro && !visitada[fila][columna] && clase1.MAPA[fila][columna] == 0) { // Solo avanza por calles nuevas.
                    visitada[fila][columna] = true; // Marca la calle alcanzada.
                    pendientes.add(new int[] {fila, columna}); // La agrega para seguir explorando.
                }
            }
        }
        for (int fila = 0; fila < lado; fila++) { // Recorre todas las filas.
            for (int columna = 0; columna < lado; columna++) { // Recorre todas las columnas.
                if (clase1.MAPA[fila][columna] == 0) { // Solo revisa calles.
                    assertTrue(visitada[fila][columna]); // Toda calle debe estar conectada con la red.
                }
            }
        }
    }

    /** Comprueba que cada destino de entrega está sobre una calle y el auto puede estar allí. */
    public void testDestinosConAccesoPorCalle() {
        clase2 juego = new clase2(); // Crea el estado del juego sin ejecutar run().
        for (float[] destino : clase4.DESTINOS) { // Revisa cada destino.
            int columna = (int) ((destino[0] + clase1.LIMITE) / clase1.CELDA); // Convierte X en columna.
            int fila = (int) ((destino[1] + clase1.LIMITE) / clase1.CELDA); // Convierte Z en fila.
            assertEquals(0, clase1.MAPA[fila][columna]); // El destino debe caer sobre una calle.
            assertTrue(juego.puedeCircular(destino[0], destino[1])); // El auto debe caber en el destino.
        }
    }

    /** Comprueba que las avenidas de los bordes son transitables de punta a punta. */
    public void testAvenidasTransitables() {
        clase2 juego = new clase2(); // Crea el estado del juego sin ejecutar run().
        for (float recorrido = -50; recorrido <= 50; recorrido += 0.25f) { // Recorre las avenidas completas.
            assertTrue(juego.puedeCircular(-50, recorrido)); // Avenida oeste.
            assertTrue(juego.puedeCircular(50, recorrido)); // Avenida este.
            assertTrue(juego.puedeCircular(recorrido, -50)); // Avenida norte.
            assertTrue(juego.puedeCircular(recorrido, 50)); // Avenida sur.
            assertTrue(juego.puedeCircular(recorrido, 10)); // Avenida interior que pasa por el centro.
        }
    }

    /** Comprueba edificios, parques, aceras y bordes del mapa. */
    public void testObstaculosYLimites() {
        clase2 juego = new clase2(); // Crea un vehículo con su radio de colisión inicial.
        assertFalse(juego.puedeCircular(-40, -40)); // Rechaza el centro de un edificio.
        assertFalse(juego.puedeCircular(0, 0)); // Rechaza el centro de la plaza central.
        assertFalse(juego.puedeCircular(-45.5f, -40)); // Rechaza una posición cuyo círculo invade la acera.
        assertTrue(juego.puedeCircular(-47, -40)); // Acepta una posición con separación suficiente de la acera.
        assertFalse(juego.puedeCircular(54, 0)); // Rechaza una posición demasiado cercana al borde derecho.
        assertFalse(juego.puedeCircular(0, -56)); // Rechaza una posición exterior al borde norte.
    }

    /** Comprueba cantidad y ubicación de las farolas: en aceras, fuera de edificios y en todos los sectores. */
    public void testFarolasEnAcerasYSectores() {
        assertTrue(clase3.LUCES.length >= 9); // Exige al menos nueve farolas.
        boolean[] sectorIluminado = new boolean[clase1.SECTORES.length]; // Marca los sectores con farola.
        for (float[] luz : clase3.LUCES) { // Revisa cada farola.
            float posteX = luz[3]; // Posición X del poste.
            float posteZ = luz[4]; // Posición Z del poste.
            assertTrue(Math.abs(luz[0]) < clase1.LIMITE && Math.abs(luz[2]) < clase1.LIMITE); // Bombilla dentro del mapa.
            int columna = (int) ((posteX + clase1.LIMITE) / clase1.CELDA); // Convierte X del poste en columna.
            int fila = (int) ((posteZ + clase1.LIMITE) / clase1.CELDA); // Convierte Z del poste en fila.
            assertTrue(clase1.MAPA[fila][columna] != 0); // El poste está sobre una acera, no sobre la calle.
            float separacionX = Math.abs(posteX - clase1.centro(columna)); // Distancia al centro de la manzana en X.
            float separacionZ = Math.abs(posteZ - clase1.centro(fila)); // Distancia al centro de la manzana en Z.
            assertTrue(separacionX > 3.5f || separacionZ > 3.5f); // Queda fuera del edificio de ancho 7.
            float brazo = (float) Math.hypot(luz[0] - posteX, luz[2] - posteZ); // Longitud del brazo.
            assertTrue(brazo > 0.5f && brazo < 2); // La bombilla cuelga cerca del poste.
            assertTrue(luz[1] > 4 && luz[1] < 6); // La bombilla está a altura de farola.
            sectorIluminado[clase1.sector(fila, columna)] = true; // Registra el sector de la farola.
        }
        for (boolean iluminado : sectorIluminado) { // Revisa cada sector.
            assertTrue(iluminado); // Todos los sectores, incluidos los nuevos, tienen farolas.
        }
    }

    /** Comprueba que cada calle tiene alguna farola cerca. */
    public void testCadaCalleTieneFarola() {
        for (int indice = 0; indice < clase1.MAPA.length; indice += 2) { // Recorre las filas y columnas de calle.
            float linea = clase1.centro(indice); // Coordenada del eje de esa calle.
            boolean filaCubierta = false; // Indica si la calle horizontal tiene farola cerca.
            boolean columnaCubierta = false; // Indica si la calle vertical tiene farola cerca.
            for (float[] luz : clase3.LUCES) { // Revisa cada farola.
                filaCubierta |= Math.abs(luz[2] - linea) <= 10; // Farola a 10 unidades o menos de la calle horizontal.
                columnaCubierta |= Math.abs(luz[0] - linea) <= 10; // Farola a 10 unidades o menos de la calle vertical.
            }
            assertTrue(filaCubierta); // La calle horizontal está iluminada.
            assertTrue(columnaCubierta); // La calle vertical está iluminada.
        }
    }

    /** Comprueba que el paso entre noche y día es gradual y termina en el ambiente elegido. */
    public void testTransicionDiaNoche() {
        clase3 juego = new clase3(); // Crea la etapa de iluminación sin abrir ventana.
        assertEquals(1f, juego.factorNoche, 0f); // Empieza de noche.
        juego.noche = false; // Simula pulsar N para pedir el día.
        juego.avanzarTransicion(0.3f); // Avanza 0.3 segundos.
        assertTrue(juego.factorNoche > 0 && juego.factorNoche < 1); // A mitad de camino: la luz cambia poco a poco.
        assertTrue(juego.lucesEncendidas()); // Al principio de la transición las farolas siguen encendidas.
        for (int cuadro = 0; cuadro < 100; cuadro++) { // Simula un par de segundos de cuadros.
            juego.avanzarTransicion(0.03f); // Avanza un cuadro.
        }
        assertEquals(0f, juego.factorNoche, 0f); // Termina de día exactamente.
        assertFalse(juego.lucesEncendidas()); // De día las farolas y ventanas se apagan.
    }

    /** Comprueba que el reinicio restaura la posición y detiene el vehículo. */
    public void testReinicio() {
        clase4 juego = new clase4(); // Crea la versión final sin inicializar OpenGL.
        juego.autoX = 12; // Simula que el auto se desplazó horizontalmente.
        juego.autoZ = 4; // Simula un desplazamiento en profundidad.
        juego.velocidad = 10; // Simula que el auto está avanzando.
        juego.angulo = 2; // Simula que el vehículo cambió de orientación.
        juego.reiniciar(); // Ejecuta el mismo reinicio que se activa con R.
        assertEquals(-50f, juego.autoX, 0f); // Exige recuperar la coordenada X inicial.
        assertEquals(50f, juego.autoZ, 0f); // Exige recuperar la coordenada Z inicial.
        assertEquals(0f, juego.velocidad, 0f); // Exige que el vehículo quede detenido.
        assertEquals(0f, juego.angulo, 0f); // Exige recuperar la dirección frontal inicial.
        assertTrue(juego.puedeCircular(juego.autoX, juego.autoZ)); // El punto de inicio está sobre la calle.
    }

    /** Comprueba que R después de ganar permite repetir todas las entregas y que los semáforos no se detienen. */
    public void testReinicioEntregas() {
        clase4 juego = new clase4(); // Crea la versión final sin inicializar OpenGL.
        juego.entregas = clase4.DESTINOS.length; // Simula una partida ganada.
        juego.tiempo = 95; // Simula el tiempo final de esa partida.
        juego.relojCiudad = 40; // Simula el reloj de los semáforos.
        juego.reiniciar(); // Ejecuta el mismo reinicio que se activa con R.
        assertEquals(0, juego.entregas); // Vuelve a la primera entrega.
        assertEquals(0f, juego.tiempo, 0f); // Reinicia el cronómetro.
        assertEquals(40f, juego.relojCiudad, 0f); // El reloj de los semáforos no se reinicia.
    }

    /** Comprueba la secuencia rojo → verde → amarillo y que los dos cabezales de un cruce nunca están en verde a la vez. */
    public void testSecuenciaSemaforo() {
        assertEquals(clase4.FASE_ROJO, clase4.faseSemaforo(0)); // Empieza en rojo.
        assertEquals(clase4.FASE_VERDE, clase4.faseSemaforo(7)); // Después pasa a verde.
        assertEquals(clase4.FASE_AMARILLO, clase4.faseSemaforo(11)); // Después a amarillo.
        assertEquals(clase4.FASE_ROJO, clase4.faseSemaforo(12.5f)); // Y vuelve a rojo.
        for (float t = 0; t < 30; t += 0.1f) { // Recorre varios ciclos.
            int fase = clase4.faseSemaforo(t); // Fase del cabezal del tráfico en Z.
            int siguiente = clase4.faseSemaforo(t + 0.1f); // Fase un instante después.
            if (fase != siguiente) { // Hubo un cambio de luz.
                assertEquals((fase + 1) % 3, siguiente); // Siempre en el orden rojo → verde → amarillo → rojo.
            }
            int cruzada = clase4.faseSemaforo(t + 6); // Fase del cabezal del tráfico en X.
            assertFalse(fase == clase4.FASE_VERDE && cruzada == clase4.FASE_VERDE); // Nunca ambos en verde.
        }
    }

    /** Comprueba que los semáforos están sobre la acera y no comparten esquina con una farola. */
    public void testSemaforosEnAcera() {
        clase4 juego = new clase4(); // Crea la versión final sin inicializar OpenGL.
        assertTrue(clase4.SEMAFOROS.length >= 9); // Hay semáforos repartidos por la ciudad.
        for (float[] semaforo : clase4.SEMAFOROS) { // Revisa cada semáforo.
            assertFalse(juego.puedeCircular(semaforo[0], semaforo[1])); // El poste no está sobre la calle.
            for (float[] farola : clase3.LUCES) { // Lo compara con cada farola.
                float distancia = (float) Math.hypot(semaforo[0] - farola[3], semaforo[1] - farola[4]); // Separación de postes.
                assertTrue(distancia > 1); // No chocan entre sí.
            }
        }
    }

    /** Comprueba que la celda central del mapa es un parque: allí se construye la plaza con la fuente. */
    public void testPlazaCentralEsParque() {
        assertEquals(2, clase1.MAPA[clase4.PLAZA][clase4.PLAZA]); // La plaza necesita una celda de parque.
        assertEquals(0f, clase1.centro(clase4.PLAZA), 0f); // Y queda en el centro de la ciudad.
        int esquinas = 0; // Cuenta las farolas en las esquinas de la plaza.
        for (float[] farola : clase3.LUCES) { // Revisa cada poste.
            if (Math.abs(Math.abs(farola[3]) - 4.3f) < 0.01f && Math.abs(Math.abs(farola[4]) - 4.3f) < 0.01f) { // Esquina de la plaza.
                esquinas++; // Suma una esquina iluminada.
            }
        }
        assertEquals(4, esquinas); // Una farola en cada esquina, sin duplicados.
    }

    /** Comprueba que los parques mezclan sakura, tajibo blanco y árbol verde común. */
    public void testEspeciesEnParques() {
        boolean[] usada = new boolean[3]; // Marca qué especies aparecen.
        for (int fila = 0; fila < clase1.MAPA.length; fila++) { // Recorre las filas del mapa.
            for (int columna = 0; columna < clase1.MAPA.length; columna++) { // Recorre las columnas.
                if (clase1.MAPA[fila][columna] != 2) { // Solo interesan los parques.
                    continue; // Pasa a la siguiente celda.
                }
                for (int indice = 0; indice < 4; indice++) { // Cuatro árboles por parque.
                    usada[clase4.especieArbol(fila, columna, indice)] = true; // Marca la especie.
                }
            }
        }
        assertTrue(usada[clase4.ARBOL_VERDE]); // Hay árboles verdes comunes.
        assertTrue(usada[clase4.ARBOL_SAKURA]); // Hay sakuras.
        assertTrue(usada[clase4.ARBOL_TAJIBO]); // Hay tajibos blancos.
    }
}
