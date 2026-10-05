// Caso limite de robustez, mismo rol que
// resources/sintactico/panico/panicoSinSincronizacionHastaEof.java para el
// sintactico: un ciclo grande (50 clases, C0 -> C1 -> ... -> C49 -> C0) para
// confirmar que el recorrido que busca ciclos de herencia termina y reporta
// un unico error (no 50, no cero) incluso cuando el ciclo es grande -- sin
// colgarse ni degradar a complejidad cuadratica. El tester correspondiente
// deberia correr con @Test(timeout = ...) como guardrail, igual que
// TesterSintacticoModoPanico.
class C0 extends C1{}
class C1 extends C2{}
class C2 extends C3{}
class C3 extends C4{}
class C4 extends C5{}
class C5 extends C6{}
class C6 extends C7{}
class C7 extends C8{}
class C8 extends C9{}
class C9 extends C10{}
class C10 extends C11{}
class C11 extends C12{}
class C12 extends C13{}
class C13 extends C14{}
class C14 extends C15{}
class C15 extends C16{}
class C16 extends C17{}
class C17 extends C18{}
class C18 extends C19{}
class C19 extends C20{}
class C20 extends C21{}
class C21 extends C22{}
class C22 extends C23{}
class C23 extends C24{}
class C24 extends C25{}
class C25 extends C26{}
class C26 extends C27{}
class C27 extends C28{}
class C28 extends C29{}
class C29 extends C30{}
class C30 extends C31{}
class C31 extends C32{}
class C32 extends C33{}
class C33 extends C34{}
class C34 extends C35{}
class C35 extends C36{}
class C36 extends C37{}
class C37 extends C38{}
class C38 extends C39{}
class C39 extends C40{}
class C40 extends C41{}
class C41 extends C42{}
class C42 extends C43{}
class C43 extends C44{}
class C44 extends C45{}
class C45 extends C46{}
class C46 extends C47{}
class C47 extends C48{}
class C48 extends C49{}
class C49 extends C0{}
