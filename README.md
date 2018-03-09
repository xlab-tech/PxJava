# pipeline

Pipeline es una libreria que intenta acercar mas java al mundo funcional. Utilizando las functions-lambda introducidas en java8.

### Qué es pipeline?
Pipeline es un orquestador de funciones para que estas sean mucho mas faciles de utilizar y poder llegar asi a resolver grandes problemas de negocio.
```
String response = new Pipeline<String>()
                .chain(x -> {
                    assertEquals(x, "inicio");
                    return "hola";
                })
                .chain(x -> {
                    assertEquals(x, "hola");
                    return "adios";
                })
                .sync().execute("inicio");
                
                Assert.assertEquals(response, "adios");
               
 ```
 
Como podemos ver pipeline nos orquesta las funciones introducidas en el methodo chain y las ejecutara una tras otra cuando realizemos **execute()**.

## Utilidades

### chain
Este metodo nos orquesta las diferentes funciones como hemos visto anteriormente pero tambien le podemos pasar mas de 1 funcion por su parametro. Podriamos escribir lo mismo que hicimos anteriormente de esta forma
```
String response = new Pipeline<String>()
                .chain(x -> {
                    assertEquals(x, "inicio");
                    return "hola";
                },x -> {
                    assertEquals(x, "hola");
                    return "adios";
                })
                .sync().execute("inicio");
                
                Assert.assertEquals(response, "adios");
               
 ```
 ### chainConcat
 Este metodo nos lanzara las funciones que hemos pasado por parametro pero las lanzara en paralelo y nos devolvera el resultado en una lista ordenada por las funciones introducidas.
 ```
List<String> response = new Pipeline<List<String>>()
                .chainConcat(x -> {
                    assertEquals(x, "inicio");
                    return "hola";
                },x -> {
                    assertEquals(x, "inicio");
                    return "adios";
                })
                .sync().execute("inicio");
                
                Assert.assertEquals(response.get(0), "hola");
                Assert.assertEquals(response.get(0), "adios");
 ```
 
 ### chainIf
 Este metodo solo nos lanzara las funciones registradas en su interior si la condicion que le pasamos por parametro es cierta.
 
 ```
 String response = new Pipeline<String>()
                .chainIf(x-> x.equals("inicio"), x -> {
                    assertEquals(x, "inicio");
                    return "hola";
                })
                .chainIf(x -> x.equals("pepe"), x -> {
                    assertEquals(x, "inicio");
                    return "adios";
                })
                .sync().execute("inicio");
                
                Assert.assertEquals(response, "hola");
 ```
 Como podemos ver la segunda funcion nunca se ejecutara ya que su entrada nunca sera pepe.
 
 ## Subscriptores/observadores y Assyncronia
 
 Los subscriptores son observadores que se lanzaran en diferentes momentos segun el metodo que utilizemos para ejecutarlos. *subscribeAfter, subscribeBefore, subscribeError, subscribeResult* . Pipeline es una libreria que trabaja en asyncrono por defecto , a no ser que utilizemos el metodo sync, por ello tenemos los subscriptores de error y resultado para poder trabajar con estos cuando pipeline haya tenido un error o haya finalizado la ajecucion.
 
  ```
 new Pipeline().chainIf(x-> x.equals("inicio"), x -> {
                    assertEquals(x, "inicio");
                    return "hola";
                }).chainIf(x -> x.equals("pepe"), x -> {
                    assertEquals(x, "inicio");
                    return "adios";
                }).subscribeAfter(x->{
                   System.out.println("parametro de salida: "x);
                }).subscribeBefore(x->{
                   System.out.println("parametro de entrada: "x);
                }).subscribeResult(x-> {
                    // Logica para contestar a front-end responseHTTP.response(msg)
                    Assert.assertEquals(x,"hola")
                }).subscribeError(x->{
                    // Logica que queremos introducir para tratar errores.
                    Assert.fail()
                }).execute("inicio");            
 ```
 
 ## Extras
 Pipeline tambien dispone de mas utilidades para relacionarse con otros pipelines y llevar asi un flujo de trabajo orientado a funciones facil y dynamico.
 
 
