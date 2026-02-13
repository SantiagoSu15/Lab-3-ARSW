
# ARSW — (Java 21): **Immortals & Synchronization** — con UI Swing
### Juan Felipe Rangel & Santiago Suarez


**Escuela Colombiana de Ingeniería – Arquitecturas de Software**  
Laboratorio de concurrencia: condiciones de carrera, sincronización, suspensión cooperativa y *deadlocks*, con interfaz **Swing** tipo *Highlander Simulator*.


---

## Requisitos

- **JDK 21** (Temurin recomendado)
- **Maven 3.9+**
- SO: Windows, macOS o Linux

---

## Cómo ejecutar

### Interfaz gráfica (Swing) — *Highlander Simulator*

**Opción A (desde `Main`, modo `ui`)**
```bash
mvn -q -DskipTests exec:java -Dmode=ui -Dcount=8 -Dfight=ordered -Dhealth=100 -Ddamage=10
```

**Opción B (clase de la UI directamente)**
```bash
mvn -q -DskipTests exec:java   -Dexec.mainClass=edu.eci.arsw.highlandersim.ControlFrame   -Dcount=8 -Dfight=ordered -Dhealth=100 -Ddamage=10
```

**Parámetros**  
- `-Dcount=N` → número de inmortales (por defecto 8)  
- `-Dfight=ordered|naive` → estrategia de pelea (`ordered` evita *deadlocks*, `naive` los puede provocar)  
- `-Dhealth`, `-Ddamage` → salud inicial y daño por golpe

### Demos teóricas (sin UI)
```bash
mvn -q -DskipTests exec:java -Dmode=demos -Ddemo=1  # 1 = Deadlock ingenuo
mvn -q -DskipTests exec:java -Dmode=demos -Ddemo=2  # 2 = Orden total (sin deadlock)
mvn -q -DskipTests exec:java -Dmode=demos -Ddemo=3  # 3 = tryLock + timeout (progreso)
```

---

## Controles en la UI

- **Start**: inicia una simulación con los parámetros elegidos.
- **Pause & Check**: pausa **todos** los hilos y muestra salud por inmortal y **suma total** (invariante).
- **Resume**: reanuda la simulación.
- **Stop**: detiene ordenadamente.

**Invariante**: con N jugadores y salud inicial H, la **suma total** de salud debe permanecer constante (salvo durante un update en curso). Usa **Pause & Check** para validarlo.

---

## Arquitectura (carpetas)

```
edu.eci.arsw
├─ app/                 # Bootstrap (Main): modes ui|immortals|demos
├─ highlandersim/       # UI Swing: ControlFrame (Start, Pause & Check, Resume, Stop)
├─ immortals/           # Dominio: Immortal, ImmortalManager, ScoreBoard
├─ concurrency/         # PauseController (Lock/Condition; paused(), awaitIfPaused())
├─ demos/               # DeadlockDemo, OrderedTransferDemo, TryLockTransferDemo
└─ core/                # BankAccount, TransferService (para demos teóricas)
```

---

# Actividades del laboratorio

## link del repositorio con el codigo del punto 1 https://github.com/juanfe-rangel/busy_wait_vs_wait_notify.git

## Parte I — (Antes de terminar la clase) `wait/notify`: Productor/Consumidor
1. Ejecuta el programa de productor/consumidor y monitorea CPU con **jVisualVM**. ¿Por qué el consumo alto? ¿Qué clase lo causa?  
2. Ajusta la implementación para **usar CPU eficientemente** cuando el **productor es lento** y el **consumidor es rápido**. Valida de nuevo con VisualVM.  
3. Ahora **productor rápido** y **consumidor lento** con **límite de stock** (cola acotada): garantiza que el límite se respete **sin espera activa** y valida CPU con un stock pequeño.
---
1. la clase que causa el alto consumo es la clase `busyspinqueue`, esto debido a que utiliza espera activa en estos fragmentos de codigo
```bash
while (true) {
    if (q.size() < capacity) {
        q.addLast(item);
        return;
    }
    Thread.onSpinWait();
}
```
y
```bash
while (true) {
    T v = q.pollFirst();
    if (v != null)
        return v;
    Thread.onSpinWait();
}
```
esto quiere decir que el hilo entra en un ciclo infinito en donde no se bloquea, duerme o libera cpu sino que se mantiene constantemente ejecutando el ciclo infinito sin cambio alguno .

---
2. tras realizar los cambios para productor lento / consumidor rapido se realizo la siguiente prueba
```bash
     -Dmode=spin \
     -Dproducers=1 \
     -Dconsumers=1 \
     -Dcapacity=2 \
     -DprodDelayMs=0 \
     -DconsDelayMs=100 \
     -DdurationSec=20 \
```
y se obtuvieron los siguientes resultados
<img width="1342" height="915" alt="image" src="https://github.com/user-attachments/assets/c62e5bd1-46bd-4349-86d7-57c613dd52c5" />

---
3. tras realizar los cambios para productor rapido / consumidor lento se realizo la siguiente prueba
```bash
     -Dmode=spin \
     -Dproducers=1 \
     -Dconsumers=1 \
     -Dcapacity=2 \
     -DprodDelayMs=100 \
     -DconsDelayMs=0 \
     -DdurationSec=20 \
```
y se obtuvieron los siguientes resultados
<img width="1342" height="915" alt="image" src="https://github.com/user-attachments/assets/d22d22e5-1764-407c-8019-518789f74837" />

## Parte II — (Antes de terminar la clase) Búsqueda distribuida y condición de parada
Reescribe el **buscador de listas negras** para que la búsqueda **se detenga tan pronto** el conjunto de hilos detecte el número de ocurrencias que definen si el host es confiable o no (`BLACK_LIST_ALARM_COUNT`). Debe:
- **Finalizar anticipadamente** (no recorrer servidores restantes) y **retornar** el resultado.  
- Garantizar **ausencia de condiciones de carrera** sobre el contador compartido.

> Puedes usar `AtomicInteger` o sincronización mínima sobre la región crítica del contador.

---

## Parte III — (Avance) Sincronización y *Deadlocks* con *Highlander Simulator*
1. Revisa la simulación: N inmortales; cada uno **ataca** a otro. El que ataca **resta M** al contrincante y **suma M/2** a su propia vida.  
2. **Invariante**: con N y salud inicial `H`, la suma total debería permanecer constante (salvo durante un update). Calcula ese valor y úsalo para validar.  
3. Ejecuta la UI y prueba **“Pause & Check”**. ¿Se cumple el invariante? Explica.  
4. **Pausa correcta**: asegura que **todos** los hilos queden pausados **antes** de leer/imprimir la salud; implementa **Resume** (ya disponible).  
5. Haz *click* repetido y valida consistencia. ¿Se mantiene el invariante?  
6. **Regiones críticas**: identifica y sincroniza las secciones de pelea para evitar carreras; si usas múltiples *locks*, anida con **orden consistente**:
   ```
   synchronized (lockA) {
     synchronized (lockB) {
       // ...
     }
   }
   ```
7. Si la app se **detiene** (posible *deadlock*), usa **`jps`** y **`jstack`** para diagnosticar.  
8. Aplica una **estrategia** para corregir el *deadlock* (p. ej., **orden total** por nombre/id, o **`tryLock(timeout)`** con reintentos y *backoff*).  
9. Valida con **N=100, 1000 o 10000** inmortales. Si falla el invariante, revisa la pausa y las regiones críticas.  
10. **Remover inmortales muertos** sin bloquear la simulación: analiza si crea una **condición de carrera** con muchos hilos y corrige **sin sincronización global** (colección concurrente o enfoque *lock-free*).  
11. Implementa completamente **STOP** (apagado ordenado).
---
1,2 y 3. se realiza una simulacion con 2 inmortales en donde cada uno tiene 10 de vida y 10 de ataque, por lo que se puede ver que luego de un ataque uno queda con 0 de vida y el otro con 10 + 10/2 (ataque)
<img width="617" height="434" alt="image" src="https://github.com/user-attachments/assets/877b6e6e-6484-4b25-be3d-a10106252a8c" />
como se puede observar la suma total de la vida de ambos inmortales no es la que deberia ser, puesto que con cada lucha este total decrece en la cantidad de ataques multiplicado por la mitad del daño de ataque. Por lo tanto se puede decir que el invariante no se cumple.

esto debido a que pese a que el juego se pausa no significa que todos los hilos se pausen, por lo tanto cuando este se detiene puede que aun haya hilos ejecutandose lo que haria que la suma de vida sea una cantidad erronea (se aprecia mejor en ejemplos con mas hilos)

4 y 5. para asegurar una pausa correcta se implemento un contador en donde cuando la bandera de pausa se activa, todos los hilos empiezan a pausarse y aumentar el contador de hilos pausados, hasta que el contador no sea igual al numero total de hilos (inmortales) no se empezara a hacer el calculo de vida.

ademas se agregaron indicadores para poder verificar la vida esperada y la vida obtenida y facilitar la identificacion de posibles condiciones carrera.
<img width="610" height="437" alt="image" src="https://github.com/user-attachments/assets/164b71c0-a5db-42bc-8a2a-5c869693c913" />

6.  El atributo de vida de cada inmortal era compartido, se decidio por cambiar a AtomicInteger.
```
synchronized (first) {
   synchronized (second) {
           if (this.health <= 0 || other.health <= 0) return;
   other.health -= this.damage;      
           this.health += this.damage / 2;   
           scoreBoard.recordFight();         
    }
}
```
```
 private AtomicInteger health;
```
TotalHealth En ImmortalManager Se volvio Sincronizado

7. Despues de pruebas no se detecto ningun deadlock.
Pruebas en el archivo threads_report.txt
![Imagen](/img/Deadloc1.png)
![Imagen](/img/Deadloc2.png)
![Imagen](/img/Deadloc3.png)

8. Para evitar Deadlocks en el modo Ingenuo se decidio usar tryLock, este se crea en el objeto 
Si logra conseguir ambos locks se ejecuta la lucha en el try y apenas acaba los libera para poder realizar otra lucha
en caso de que no revisa si es dueño de algun lock para liberarlo y poder volver a buscar otra lcuha

```
private ReentrantLock lock = new ReentrantLock();

private void fightNaive(Immortal other) throws InterruptedException {
    if((lock.tryLock(1000, TimeUnit.MILLISECONDS)) && (other.lock.tryLock(1000, TimeUnit.MILLISECONDS))){
      try{
       .... 
      }finally{
        lock.unlock();
        other.lock.unlock();
      }
    }else{
      if(lock.isHeldByCurrentThread()) lock.unlock();
      if(other.lock.isHeldByCurrentThread()) other.lock.unlock();
    }
  }
```
9. Se hizo la prueba con max 1000 debido que no dejaba con un numero mayor
![imagen](/img/img.png)
10. Para eliminar los inmortales muertos se decidio cambiar de coleccion: List -> CopyOnWriteArrayList, ya que es una coleccion concurrent
Para manejar la limpieza se decidio crear un hilo aparte para limpiar cada vez que un inmortal muere:
```
futures.add(exec.submit(()->{
      controller.registerThread();
      try{
        while (!exec.isShutdown()){
          controller.awaitIfPaused();
          if (!controller.paused()) {
            for(Immortal im : population){
              if(im.getHealth() <= 0){
                population.remove(im);
                im.setRunning(false);
              }
            }
          }
          Thread.sleep(1000);
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }finally {
        controller.unregisterThread();
      }

    }));
```
![imagen](/img/img_1.png)
![imagen](/img/img_2.png)
11. En stop Ahora cada hilo su banderita se pone en false, espera que los hilos terminen  y hace que el ExecutorService no reciba nuevas tareas y limpia recursos
```

for (Immortal im : population) {
im.stop();
}

    if (exec != null) {
      exec.shutdown();
      try {
        if (!exec.awaitTermination(5, TimeUnit.SECONDS)) {
          exec.shutdownNow();
        }
      } catch (InterruptedException e) {
        exec.shutdownNow();
        Thread.currentThread().interrupt();
      }
      exec = null;
    }

    futures.clear();
}
```

---
