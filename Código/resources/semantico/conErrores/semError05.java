///[ErrorSem:ERR_TIPO_NO_DECLARADO|9]
// Robustez: la cadena se rompe a mitad de camino por un tipo no declarado
// ("Foo"), no por un ciclo. El recorrido que busca ciclos tiene que
// distinguir "llegue a una clase que ya habia visitado" (ciclo) de "el
// padre ni siquiera existe en la tabla" (tipo no declarado) -- si no se
// distinguen, una implementacion ingenua puede reportar el error
// equivocado o, peor, colgarse buscando un padre que nunca aparece.
class A1 extends B1{}
class B1 extends Foo{}
