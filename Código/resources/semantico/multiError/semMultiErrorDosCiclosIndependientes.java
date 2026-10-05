// Dos ciclos de herencia independientes en el mismo archivo (A<->B por un
// lado, X<->Y por el otro): tienen que reportarse los DOS, cada uno dentro
// de su propio componente conexo, sin que la deduplicacion de un ciclo
// "tape" al otro (Logro 4, multi-deteccion de errores semanticos -- ver
// seccion 5.4 de propuesta_casos_test_semantico.md). Harness esperado:
// containsString("[ErrorSem:ERR_HERENCIA_CICLICA|10]") (cierra en B) y
// containsString("[ErrorSem:ERR_HERENCIA_CICLICA|12]") (cierra en Y), los
// dos en la misma corrida.
class A extends B{}
class B extends A{}
class X extends Y{}
class Y extends X{}
