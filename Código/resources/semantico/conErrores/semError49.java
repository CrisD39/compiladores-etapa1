///[ErrorSem:ERR_TIPO_NO_DECLARADO|11]
// Logro 5: dos niveles de anidamiento. "Caja" y "Lista" existen, "Foo" (en
// el segundo nivel) no -- prueba que la validacion recorre todo el
// argumento, no solo el primer nivel.
class Caja<T>{
}

class Lista<T>{
}

class Contenedor{ Caja<Lista<Foo>> contenido; }
