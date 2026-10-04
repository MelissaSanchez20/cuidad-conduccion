# Ciudad y conducción con OpenGL: cuatro clases

Proyecto de enseñanza basado en `AppCamara.java` y `AppLaberinto.java` de tus clases: Java 17, Maven, LWJGL 3.3.3, GLFW, OpenGL 3.3 Core, shaders GLSL, VAO y VBO. Toda la geometría se genera con código; no requiere imágenes ni modelos externos.

## Ejecutar

Abre una terminal **en esta carpeta**, donde está `pom.xml`. Necesitas un JDK 17 o superior y Maven (`java -version`, `mvn -version`). La primera compilación descarga las dependencias.

```sh
mvn compile exec:exec -DmainClass=com.graphics.clase1
mvn compile exec:exec -DmainClass=com.graphics.clase2
mvn compile exec:exec -DmainClass=com.graphics.clase3
mvn compile exec:exec -DmainClass=com.graphics.clase4
```

Ejecuta un comando por vez; ESC cierra la ventana. `mvn compile exec:exec` abre la versión final por defecto. En macOS, el perfil Maven agrega `-XstartOnFirstThread`. Si ejecutas desde un IDE en Mac, agrega ese argumento a las opciones de la JVM. Usa `exec:exec`, porque `exec:java` no inicia el proceso de esa forma.

Incluye bibliotecas nativas para macOS Intel/Apple Silicon, Windows x64 y Linux x64/ARM64. Se necesita una sesión gráfica y un controlador compatible con OpenGL 3.3. No está diseñado para ejecutarse en un servidor sin pantalla.

## Mejoras realizadas sobre el proyecto base

### 1. Ciudad ampliada
- Mapa de **11 × 11 celdas** (110 × 110 unidades, `LIMITE = 55`), con el mismo tamaño de celda (10).
- 6 avenidas en cada sentido y 25 manzanas: **19 edificios y 6 parques**, todas conectadas por la red vial.
- Cinco sectores: Centro, Barrio Norte, Barrio Sur, Distrito Oeste y Distrito Este. Cada uno tiene su propia
  paleta de color y rango de alturas (`sector()`, `alturaEdificio()`, `colorEdificio()` en clase1); el Centro tiene rascacielos.
- Suelo, límites, colisiones, cámara aérea y escala del minimapa (`uEscalaMapa`) se calculan a partir del mapa.
- El auto parte de la esquina suroeste (-50, 50) y hay **4 entregas** sobre calles de sectores distintos.

### 2. Iluminación
- **21 farolas** generadas automáticamente junto a las intersecciones (patrón de tablero de ajedrez), con base,
  poste, brazo sobre la calle y bombilla. Su luz se atenúa con la distancia y forma charcos de luz cálida.
- **Transición suave día/noche** con N: luz ambiente, sol/luna, farolas y cielo cambian gradualmente.
- Ambiente hemisférico (las caras que miran al cielo reciben más luz), sol cálido y luna azulada.
- Reflejos especulares Blinn-Phong y niebla atmosférica que funde lo lejano con el cielo.
- Dos **faros spotlight** con cono suave y alcance limitado (se apagan entre 20 y 28 unidades).
- Halos de luz (billboard con mezcla aditiva) en farolas, faros y luces traseras; **luces de freno** al frenar.
- Las bombillas usan emisión propia; el entorno recibe la luz calculada en el shader.

### 3. Funciones urbanas
- **Parques estilo jardín japonés:** mezcla de **sakura** (`#FF69B4`), **tajibo blanco** (`#FDFBF7`) y árbol verde común,
  con pétalos caídos bajo los árboles en flor, sendero de losas (tobi-ishi), **linterna de piedra (tōrō)** que se
  enciende de noche dos **bancos japoneses** de listones de madera sobre bloques de piedra, **arbustos** (algunos con flores rosadas),
  **basureros** octogonales y un **bebedero**. El césped es verde lechuga (`#8CC63F`).
- **Plaza central** (parque del centro, en (0, 0)): piso de piedra, **fuente de agua** de tres niveles con chorro y
  cascadas animadas (de noche el agua se ilumina), **4 iluminarias** de jardín con halos nocturnos, 4 bancos mirando
  a la fuente, sakuras y tajibos en diagonal, arbustos con flores rosadas y blancas y una farola de calle en cada esquina.
- **Auto** estilo Mitsubishi Lancer Evolution X en **fucsia** (`#FF00FF`): parrilla negra trapezoidal, tomas de aire en
  el capó, alerón trasero, faldones, espejos, escape y llantas, todo hecho con cajas.
- **Ventanas:** vidrio oscuro de día; de noche unas habitaciones encendidas y otras apagadas (patrón fijo por edificio).
- **Pasos peatonales** en ambos extremos de cada tramo de calle, junto a los cruces, con franjas paralelas al tráfico.
- **18 semáforos** en las esquinas sin farola, con dos cabezales de fases opuestas (rojo → verde → amarillo, ciclo de 12 s).
  Usan un reloj propio (`relojCiudad`) que no se detiene al ganar.
- **Minimapa** con la ciudad completa, el auto (posición y orientación), el destino activo y una **N** que marca el norte arriba.

## Cómo se acumulan los avances

```text
clase1: ciudad, mapa y renderizador
  └── clase2 extends clase1: auto, conducción y colisiones
        └── clase3 extends clase2: iluminación
              └── clase4 extends clase3: detalles urbanos, entregas y minimapa
```

Cada archivo tiene su propio `main` y puede ejecutarse como una etapa distinta dentro del proyecto. Las etapas posteriores necesitan los archivos anteriores: `extends` reutiliza el avance y `super.escena()` lo dibuja antes de añadir contenido. No son cuatro copias independientes. Así, en clase2 se explica el auto sin repetir la configuración de OpenGL. Se conservaron los nombres en minúscula solicitados; en proyectos Java es habitual usar `Clase1`.

## Cómo leer el código comentado

Los cuatro archivos están divididos en secciones numeradas. Cada instrucción tiene un comentario en español; cada método indica su propósito. Las llaves y líneas vacías solo separan bloques. Los shaders también están explicados línea por línea dentro de sus cadenas GLSL.

- **clase1:** variables → inicio/ciclo/limpieza → teclado/cámara → ciudad → cajas/uniforms → shaders → vértices del cubo.
- **clase2:** variables del auto → teclado → movimiento → colisiones → cámara → piezas del vehículo.
- **clase3:** luces → controles → envío de datos → farolas → shader de iluminación.
- **clase4:** partida → controles → entregas → destino → decoración → minimapa.

Se usa una instrucción por línea, condiciones con llaves y cálculos intermedios con nombres descriptivos. El cubo contiene sus 36 vértices explícitos, como en los ejemplos originales. En `caja()` y `pieza()`, los argumentos mantienen este orden: posición X/Y/Z, tamaño X/Y/Z y color rojo/verde/azul; `cajaGirada()` agrega el ángulo al final.

## Guion de enseñanza

### Clase 1 — Crear primero la ciudad

**Resultado:** una ciudad 3D con calles conectadas, marcas viales, edificios, aceras y parques. Las flechas izquierda/derecha orbitan la cámara.

1. Leer `MAPA`: 0 es calle, 1 edificio y 2 parque. Las filas corresponden a Z y las columnas a X; Y es altura.
2. Explicar `centro()`: convierte índices de matriz a coordenadas del mundo. Cada celda mide 10 unidades y el mapa ocupa 110 × 110.
3. Revisar `crearCubo()`: posiciones y normales en VBO, atributos en VAO y 36 vértices.
4. Seguir `caja()` y `cajaGirada()`: un cubo unitario se transforma en asfalto, edificio o línea.
5. Leer `vertexShader()`: modelo, cámara y perspectiva. `uMapa` deja preparado el segundo tipo de proyección que se usa en clase4.
6. Recorrer `run()`, `iniciar()`, `loop()`, `dibujarFrame()` y `limpiar()`: eventos, actualización, limpieza, dibujo y presentación.

**Ejercicio:** cambiar una manzana de edificio a parque y variar las alturas. Mantener calles conectadas. Las normales se preparan aquí, pero la iluminación se introduce en clase3.

### Clase 2 — Crear y mover el auto

**Resultado:** auto fucsia estilo Lancer Evo X con cabina, alerón, ruedas y faros, cámara de seguimiento y colisiones con manzanas y borde.

1. Leer `dibujarAuto()` y `pieza()`: las piezas usan coordenadas locales que giran y se trasladan juntas.
2. Estudiar `actualizar(deltaTime)`: aceleración, resistencia, freno y límites de velocidad.
3. Relacionar seno/coseno con la dirección del auto. Su frente local es -Z; el giro está en radianes.
4. Explicar el giro proporcional a la velocidad y el cambio de dirección al retroceder.
5. Revisar `puedeCircular()`: círculo contra rectángulo, punto más cercano y distancia al cuadrado.
6. Alternar cámaras con C para observar la conducción en el mapa.

**Ejercicio:** modificar aceleración y resistencia y comparar el manejo. El círculo de colisión es conservador para contener todas las piezas. El choque detiene al auto; no hay rebotes. El paso temporal se limita para evitar atravesar una manzana tras una pausa larga.

### Clase 3 — Focos e iluminación

**Resultado:** ambiente nocturno, veintiún farolas que iluminan superficies, faros del vehículo con alcance limitado, halos de luz, luces de freno y transición suave día/noche.

1. Comparar el shader de color plano de clase1 con `fragmentShader()` de clase3.
2. Estudiar normales y Lambert: `max(dot(normal, direccionLuz), 0)`.
3. Revisar ambiente y luz direccional del sol.
4. Relacionar las coordenadas de `LUCES` con los postes dibujados en `escena()`.
5. Analizar cómo disminuye la luz con la distancia.
6. Explicar el cono de los faros con producto escalar y `smoothstep`.
7. Comparar emisión de la bombilla con la luz calculada sobre el suelo. Son fenómenos distintos.

**Ejercicio:** variar el color y la atenuación de una farola. La iluminación es local, sin sombras ni oclusión: una luz puede atravesar un edificio. Implementar shadow maps queda como ampliación. El color del cielo también cambia con la transición día/noche.

### Clase 4 — Ciudad final y minimapa

**Resultado:** parques estilo japonés (sakura, tajibo blanco, bancos y tōrō), ventanas iluminadas, pasos peatonales en los cruces, semáforos animados, minimapa y juego de cuatro entregas.

1. Estudiar `decorarCiudad()` y sus métodos `dibujarParque()` (con `dibujarArbol()`, `dibujarBanco()` y `dibujarToro()`), `dibujarVentanas()`, `dibujarPasosPeatonales()` y `dibujarSemaforo()`: composición de objetos reutilizando cajas y la matriz.
2. Leer `DESTINOS` y `actualizar()`: acercarse a menos de 3 unidades y frenar a menos de 1 unidad/segundo completa una entrega.
3. Seguir `dibujarFrame()`: primero la cámara principal, después una vista ortográfica en otro viewport.
4. Explicar `glScissor`: permite borrar solo el recuadro del minimapa y su profundidad.
5. Mostrar por qué se restauran viewport, scissor y `uMapa` al terminar.
6. Seguir el indicador cian del auto y su punta blanca; la marca dorada indica el destino activo y la N el norte.
7. Leer `faseSemaforo()`: el módulo del reloj da la fase; el cabezal cruzado usa el reloj desplazado 6 s.

**Ejercicio:** agregar una quinta entrega sobre una calle o modificar el tamaño del minimapa. El minimapa mantiene el norte (-Z) arriba. El destino se muestra también como baliza dorada en el mundo. Al completar las cuatro entregas aparece GANASTE en el título; R reinicia.

## Controles

| Tecla | Acción | Desde |
|---|---|---|
| Izquierda / derecha | Orbitar la ciudad | Clase 1 únicamente |
| W / S o arriba / abajo | Acelerar / frenar y retroceder | Clase 2 |
| A / D o izquierda / derecha | Girar mientras el auto se mueve | Clase 2 |
| Espacio | Freno (enciende las luces de freno) | Clase 2 |
| C | Cámara de seguimiento / aérea oblicua | Clase 2 |
| R | Reiniciar auto y, en clase4, las entregas | Clase 2 |
| N | Día / noche | Clase 3 |
| F | Encender / apagar faros | Clase 3 |
| M | Mostrar / ocultar minimapa | Clase 4 |
| ESC | Salir | Todas |

El título de la ventana muestra velocidad en km/h (se supone una unidad = un metro), luces, entregas y tiempo. Puede truncarse si la ventana es pequeña. La velocidad máxima real es algo menor que el límite por la resistencia aplicada. Los semáforos son decorativos: cambian de color pero no bloquean al vehículo. No hay tráfico, peatones, audio, sombras, modelos importados ni ruedas animadas; este es el alcance del ejemplo didáctico finalizado.

## Verificación

```sh
mvn test
```

Las pruebas de lógica comprueban, sin abrir ventanas, el tamaño del mapa y la cantidad de edificios y parques, la variación de alturas y colores, que la red de calles esté conectada, que los destinos sean accesibles, la ubicación de las farolas en todos los sectores, la transición día/noche, los márgenes de colisión, el reinicio (incluidas las entregas), la secuencia de los semáforos, su ubicación en las aceras y la mezcla de especies de árboles. Para un arranque gráfico breve puede pasarse `-Ddemo.frames=6` a la **JVM del juego**; al llegar a ese número de cuadros la ventana se cierra. Esta prueba necesita pantalla y comprueba también compilación/enlace de shaders y errores OpenGL.

Práctica manual: ejecutar cada etapa; conducir y chocar con una acera; retroceder; cambiar cámara; alternar N/F; redimensionar la ventana; alternar M; completar las cuatro entregas y reiniciar con R.

Verificado: las cuatro etapas abrieron un contexto gráfico, dibujaron varios cuadros y cerraron sin errores OpenGL (incluida una GPU AMD en Windows; por eso la línea `#version` de los shaders va sola, sin comentarios). Con Java 25 y LWJGL 3.3.3 aparecen advertencias de acceso nativo, Unsafe y versión JNI; para impartir las clases se recomienda usar JDK 17, la versión objetivo de los ejemplos originales. El arranque breve no sustituye completar manualmente el recorrido.
