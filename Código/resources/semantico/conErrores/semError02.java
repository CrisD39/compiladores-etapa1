///[ErrorSem:ERR_HERENCIA_CICLICA|7]
// Ciclo indirecto de longitud 2. Convencion de reporte: caminando el grafo
// de herencia en orden de declaracion (A primero, por eso arranca ahi), el
// ciclo se cierra en la ULTIMA clase visitada antes de volver a encontrar
// una ya vista -- acá, B (linea 7), porque B.extends apunta de nuevo a A.
class A1 extends B1{}
class B1 extends A1{}
