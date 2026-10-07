///[Error:sealed|5]
// Modificador 'sealed' repetido antes de class/interface -- ya no cae en
// la cascada generica de modo panico; ahora reporta un unico error claro
// y la declaracion real que sigue se termina parseando bien.
sealed sealed class Figura permits Circulo {

}
