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

    /** Comprueba que el movimiento usa deltaTime: a 30 o a 60 cuadros por segundo el auto recorre casi lo mismo. */
    public void testMovimientoIndependienteDeFps() {
        clase2 lento = new clase2(); // Simula una computadora a 30 cuadros por segundo.
        clase2 rapido = new clase2(); // Simula una computadora a 60 cuadros por segundo.
        for (int cuadro = 0; cuadro < 30; cuadro++) { // Un segundo a 30 FPS.
            lento.mover(1, 0, false, 1f / 30); // Acelera recto por la avenida oeste.
        }
        for (int cuadro = 0; cuadro < 60; cuadro++) { // Un segundo a 60 FPS.
            rapido.mover(1, 0, false, 1f / 60); // Misma orden, pasos más cortos.
        }
        assertTrue(rapido.velocidad > 5); // El auto realmente aceleró.
        assertEquals(rapido.velocidad, lento.velocidad, 0.3f); // Velocidad casi igual.
        assertEquals(rapido.autoZ, lento.autoZ, 0.3f); // Distancia recorrida casi igual.
        assertEquals(rapido.autoX, lento.autoX, 0.001f); // Ninguno se desvió.
    }

    /** Comprueba que el título de clase3 muestra primero el estado (velocidad, sector, luces) y después los controles. */
    public void testIndicadorLegible() {
        clase3 juego = new clase3(); // Etapa sin panel: el título es su único indicador.
        juego.velocidad = 10; // 36 km/h.
        String titulo = juego.textoIndicador(); // Arma el texto sin abrir ventana.
        int controles = titulo.indexOf("||"); // Separador entre estado y controles.
        assertTrue(controles > 0); // Existe la parte de controles.
        assertTrue(titulo.indexOf("36 km/h") < controles); // La velocidad está en el estado.
        assertTrue(titulo.indexOf("Barrio Sur") < controles); // El auto parte en el Barrio Sur.
        assertTrue(titulo.indexOf("F faros") > controles); // Las teclas van al final.
        for (char letra : titulo.toCharArray()) { // Revisa cada carácter del título.
            assertTrue(letra < 128); // Solo ASCII: se ve bien con cualquier codificación.
        }
    }

    /** Comprueba que el título de clase4 es corto: nombre del juego y estado de la partida, sin controles. */
    public void testIndicadorJuego() {
        clase4 juego = new clase4(); // Crea la versión final sin inicializar OpenGL.
        assertEquals("Luces de Neon | MENU", juego.textoIndicador()); // Portada.
        juego.estado = clase4.JUGANDO; // Empieza la partida.
        assertEquals("Luces de Neon | Entregas 0/4", juego.textoIndicador()); // Progreso.
        juego.estado = clase4.PAUSA; // Pausa.
        assertEquals("Luces de Neon | PAUSA | Entregas 0/4", juego.textoIndicador()); // Pausa y progreso.
    }

    /** Comprueba el texto de victoria del título. */
    public void testIndicadorVictoria() {
        clase4 juego = new clase4(); // Crea la versión final sin inicializar OpenGL.
        juego.estado = clase4.JUGANDO; // Partida en curso.
        juego.entregas = clase4.DESTINOS.length; // Simula todas las entregas hechas.
        juego.tiempo = 83.7f; // Tiempo final.
        String titulo = juego.textoIndicador(); // Arma el texto.
        assertTrue(titulo.contains("GANASTE en 83 s")); // Muestra la victoria y el tiempo.
        assertFalse(titulo.contains("Entregas")); // Ya no muestra el progreso.
    }

    /** Comprueba que hay al menos tres vehículos autónomos y que son los tres modelos pedidos. */
    public void testTraficoTresModelos() {
        clase4 juego = new clase4(); // Crea la versión final sin inicializar OpenGL.
        assertTrue(juego.trafico.length >= 3); // Mínimo tres vehículos.
        boolean[] modelos = new boolean[3]; // Marca qué modelos aparecen.
        for (clase4.Vehiculo vehiculo : juego.trafico) { // Recorre el tráfico.
            modelos[vehiculo.modelo] = true; // Marca su modelo.
        }
        assertTrue(modelos[clase4.AVENTADOR] && modelos[clase4.MUSTANG] && modelos[clase4.JIMNY]); // Los tres modelos.
    }

    /** Simula 200 s de tráfico: siempre sobre la calle, girando, dando vueltas y sin chocar entre ellos. */
    public void testTraficoPorLasCalles() {
        clase4 juego = new clase4(); // El jugador queda quieto en su punto de partida.
        int cantidad = juego.trafico.length; // Número de vehículos.
        java.util.List<java.util.Set<String>> direcciones = new java.util.ArrayList<>(); // Sentidos recorridos por cada uno.
        for (int indice = 0; indice < cantidad; indice++) { // Prepara un conjunto por vehículo.
            direcciones.add(new java.util.HashSet<>()); // Conjunto vacío.
        }
        for (int cuadro = 0; cuadro < 200 * 60; cuadro++) { // 200 segundos a 60 cuadros por segundo.
            juego.avanzarTrafico(1f / 60); // Mueve el tráfico un cuadro.
            for (int indice = 0; indice < cantidad; indice++) { // Revisa cada vehículo.
                clase4.Vehiculo vehiculo = juego.trafico[indice]; // Vehículo actual.
                assertTrue(clase2.libreDeManzanas(vehiculo.x, vehiculo.z, 1.2f)); // Sobre la calle y dentro del mapa.
                direcciones.get(indice).add(Math.round(vehiculo.dirX) + "," + Math.round(vehiculo.dirZ)); // Sentido del tramo.
                for (int otro = indice + 1; otro < cantidad; otro++) { // Compara con los demás.
                    clase4.Vehiculo segundo = juego.trafico[otro]; // Otro vehículo.
                    assertTrue(Math.hypot(vehiculo.x - segundo.x, vehiculo.z - segundo.z) > 2.4); // No se superponen.
                }
            }
        }
        for (int indice = 0; indice < cantidad; indice++) { // Revisa el recorrido de cada vehículo.
            assertTrue(direcciones.get(indice).size() >= 3); // Giró en varias esquinas.
            assertTrue(juego.trafico[indice].vueltas >= 1); // Completó la ruta: no quedó trabado.
        }
    }

    /** Comprueba que el jugador no puede meterse dentro de un vehículo autónomo. */
    public void testJugadorNoAtraviesaTrafico() {
        clase4 juego = new clase4(); // Crea la versión final sin inicializar OpenGL.
        clase4.Vehiculo vehiculo = juego.trafico[0]; // Toma un vehículo.
        assertFalse(juego.puedeCircular(vehiculo.x, vehiculo.z + 1)); // Acercarse hasta tocarlo está prohibido.
        assertTrue(juego.puedeCircular(juego.autoX, juego.autoZ)); // El punto de partida sigue libre.
    }

    /** Comprueba la fuente de píxeles del panel: todos los caracteres que usa existen y tienen 3 × 5 píxeles. */
    public void testFuentePanel() {
        String usados = "0123456789VELOCIDADKM/HREVLUCESFAROSONOFFNOCHEDIAFRENOMAPA "; // Texto que escribe el panel.
        for (char letra : usados.toCharArray()) { // Revisa cada carácter.
            assertTrue("Falta " + letra, clase4.FUENTE.containsKey(letra)); // Existe en la fuente.
        }
        for (String glifo : clase4.FUENTE.values()) { // Revisa cada glifo.
            assertEquals(15, glifo.length()); // Cinco filas de tres píxeles.
            assertTrue(glifo.matches("[01]+")); // Solo píxeles encendidos o apagados.
        }
        assertEquals(15f, clase4.anchoTexto("KM/H", 1), 0f); // Cuatro caracteres de 3 más 3 separaciones.
    }

    /** Comprueba que el panel y el título de clase3 muestran la misma velocidad en km/h, también en reversa. */
    public void testKilometrosPorHora() {
        clase4 juego = new clase4(); // Crea la versión final sin inicializar OpenGL.
        juego.velocidad = 10; // 10 m/s.
        assertEquals(36, juego.kilometrosPorHora()); // 36 km/h.
        juego.velocidad = -5; // Reversa a 5 m/s.
        assertEquals(18, juego.kilometrosPorHora()); // Se muestra sin signo.
        clase3 etapa = new clase3(); // Etapa que muestra la velocidad en el título.
        etapa.velocidad = -5; // Misma reversa.
        assertTrue(etapa.textoIndicador().contains("18 km/h")); // El título usa el mismo valor.
    }

    /** Comprueba la portada: empieza en el menú, la selección es circular y ENTER sobre JUGAR empieza la partida. */
    public void testMenuInicio() {
        clase4 juego = new clase4(); // Crea la versión final sin inicializar OpenGL.
        assertEquals(clase4.INICIO, juego.estado); // Arranca en la portada.
        assertEquals("JUGAR", juego.opcionesActuales()[juego.opcion]); // JUGAR seleccionado.
        juego.tecla(org.lwjgl.glfw.GLFW.GLFW_KEY_UP); // Desde la primera opción sube a la última.
        assertEquals("SALIR", juego.opcionesActuales()[juego.opcion]); // Selección circular.
        juego.tecla(org.lwjgl.glfw.GLFW.GLFW_KEY_DOWN); // Vuelve a la primera.
        assertEquals("JUGAR", juego.opcionesActuales()[juego.opcion]); // JUGAR otra vez.
        juego.tecla(org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER); // Elige JUGAR.
        assertEquals(clase4.JUGANDO, juego.estado); // La partida empieza.
    }

    /** Comprueba que la pausa congela cronómetro, semáforos y tráfico, y que P la quita. */
    public void testPausaCongela() {
        clase4 juego = new clase4(); // Crea la versión final sin inicializar OpenGL.
        juego.tecla(org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER); // Empieza la partida.
        juego.tecla(org.lwjgl.glfw.GLFW.GLFW_KEY_P); // Pausa.
        assertEquals(clase4.PAUSA, juego.estado); // Menú de pausa abierto.
        float trafico = juego.trafico[0].x + juego.trafico[0].z; // Posición de un vehículo.
        juego.actualizar(1); // Pasa un segundo en pausa.
        assertEquals(0f, juego.tiempo, 0f); // El cronómetro no avanzó.
        assertEquals(0f, juego.relojCiudad, 0f); // Los semáforos no avanzaron.
        assertEquals(trafico, juego.trafico[0].x + juego.trafico[0].z, 0f); // El tráfico no se movió.
        juego.tecla(org.lwjgl.glfw.GLFW.GLFW_KEY_P); // Quita la pausa.
        assertEquals(clase4.JUGANDO, juego.estado); // Vuelve al juego.
    }

    /** Comprueba que REINICIAR desde la pausa vuelve a empezar las entregas. */
    public void testPausaReiniciar() {
        clase4 juego = new clase4(); // Crea la versión final sin inicializar OpenGL.
        juego.tecla(org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER); // Empieza la partida.
        juego.entregas = 2; // Simula dos entregas hechas.
        juego.tecla(org.lwjgl.glfw.GLFW.GLFW_KEY_P); // Pausa.
        juego.tecla(org.lwjgl.glfw.GLFW.GLFW_KEY_DOWN); // Selecciona REINICIAR.
        assertEquals("REINICIAR", juego.opcionesActuales()[juego.opcion]); // Opción correcta.
        juego.tecla(org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER); // La elige.
        assertEquals(0, juego.entregas); // Entregas desde cero.
        assertEquals(clase4.JUGANDO, juego.estado); // De vuelta al juego.
    }

    /** Comprueba que todos los textos de los menús se pueden escribir con la fuente de píxeles. */
    public void testTextosMenuEnFuente() {
        java.util.List<String> textos = new java.util.ArrayList<>(java.util.Arrays.asList(clase4.AYUDA)); // Controles.
        textos.addAll(java.util.Arrays.asList(clase4.OPCIONES_INICIO)); // Opciones de la portada.
        textos.addAll(java.util.Arrays.asList(clase4.OPCIONES_PAUSA)); // Opciones de la pausa.
        textos.add(clase4.PIE_MENU); // Pie del menú.
        textos.add("LUCES DE NEON CONDUCE Y COMPLETA 4 ENTREGAS PAUSA GANASTE EN TIEMPO S CONTROLES >"); // Títulos.
        for (String texto : textos) { // Revisa cada texto.
            for (char letra : texto.toCharArray()) { // Y cada carácter.
                assertTrue("Falta " + letra, clase4.FUENTE.containsKey(letra)); // Existe en la fuente.
            }
        }
    }

    /** Comprueba los carteles: nombran los cinco sectores, el texto cabe y el poste está en la acera, lejos de otros postes. */
    public void testCartelesDeSectores() {
        java.util.Set<String> nombrados = new java.util.HashSet<>(); // Sectores que aparecen en algún cartel.
        for (int indice = 0; indice < clase4.CARTELES.length; indice++) { // Revisa cada pórtico.
            float[] cartel = clase4.CARTELES[indice]; // Centro y dirección.
            assertTrue(clase2.libreDeManzanas(cartel[0], cartel[1], 1)); // El centro está sobre la calle.
            for (String texto : clase4.TEXTOS_CARTELES[indice]) { // Ambas caras.
                nombrados.add(texto); // Registra el sector.
                for (String linea : clase4.lineasCartel(texto)) { // Cada renglón.
                    assertTrue(clase4.anchoTexto(linea, clase4.PIXEL_CARTEL) < clase4.LARGO_TABLERO - 0.15f); // Cabe en el tablero.
                }
                assertEquals(texto, clase1.SECTORES[clase4.indiceSector(texto)].toUpperCase()); // Tiene color de sector.
                for (char letra : texto.toCharArray()) { // Cada carácter.
                    assertTrue(clase4.FUENTE.containsKey(letra)); // Existe en la fuente.
                }
            }
            float lado = cartel[3]; // Acera del poste.
            float posteX = cartel[0] + (cartel[2] == 1 ? lado * clase4.SEPARACION_POSTES : 0); // Poste en X.
            float posteZ = cartel[1] + (cartel[2] == 1 ? 0 : lado * clase4.SEPARACION_POSTES); // Poste en Z.
            assertFalse(clase2.libreDeManzanas(posteX, posteZ, 0.2f)); // Sobre la acera, no en la calzada.
            for (float[] farola : clase3.LUCES) { // Lejos de las farolas.
                assertTrue(Math.hypot(posteX - farola[3], posteZ - farola[4]) > 1.5); // Sin choque.
            }
            for (float[] semaforo : clase4.SEMAFOROS) { // Lejos de los semáforos.
                assertTrue(Math.hypot(posteX - semaforo[0], posteZ - semaforo[1]) > 1.5); // Sin choque.
            }
        }
        for (String sector : clase1.SECTORES) { // Los cinco sectores.
            assertTrue(sector, nombrados.contains(sector.toUpperCase())); // Tienen su cartel.
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
