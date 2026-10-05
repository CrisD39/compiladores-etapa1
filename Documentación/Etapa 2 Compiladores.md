# Etapa 2 Compiladores

# Logros apuntados

- MiniLambdas  
- Variables locales clasicas  
- Imbatibilidad Sintactica  
- Well Done  
- Visibilidad Mejorada  
- Recuperación de Errores Modo Panico  
- Fors\! E2  
- Generecidad Avanzada E2  
- Atributos Inicilizados  
- Inicializacion de arreglos E2  
- Operador Ternario E2  
- Operadores Posfijos E2

# Gramatica

\<Inicial\> ::= \<ListaClases\> eof

\<ListaClases\> ::= \<Clase\> \<ListaClases\> | \<Interfaz\> \<ListaClases\> | ϵ

\<Clase\> ::= class idClase \<GenericidadOpcional\> \<HerenciaOpcional\> { \<ListaMiembros\> }

\<Interfaz\> ::= interface idClase \<GenericidadOpcional\> \<ExtensionOpcional\> { \<ListaMetodosInterfaz\> }

\<GenericidadOpcional\> ::= \< idGen \> | ϵ

\<HerenciaOpcional\> ::= extends \<TipoReferencia\> | implements \<TipoReferencia\> | ϵ

\<ExtensionOpcional\> ::= extends \<TipoReferencia\> | ϵ

\<ListaMiembros\> ::= \<Miembro\> \<ListaMiembros\> | ϵ

\<ListaMetodosInterfaz\> ::= \<MetodoInterfaz\> \<ListaMetodosInterfaz\> | ϵ

\<Miembro\> ::= \<Visibilidad\> \<CuerpoMiembro\>

\<Visibilidad\> ::= public | private | ϵ

\<CuerpoMiembro\> ::= static \<TipoMetodo\> idMetVar \<ArgsFormales\> \<Bloque\>  
\<CuerpoMiembro\> ::= void idMetVar \<ArgsFormales\> \<Bloque\>  
\<CuerpoMiembro\> ::= \<TipoPrimitivo\> \<DimensionesOpcionales\> idMetVar \<RestoMiembro\>  
\<CuerpoMiembro\> ::= idGen \<DimensionesOpcionales\> idMetVar \<RestoMiembro\>  
\<CuerpoMiembro\> ::= idClase \<TrasIdClaseMiembro\>

\<TrasIdClaseMiembro\> ::= \<ArgsFormales\> \<Bloque\>  
\<TrasIdClaseMiembro\> ::= \<TipoGenericoOpcional\> \<DimensionesOpcionales\> idMetVar \<RestoMiembro\>

\<RestoMiembro\> ::= ; | \<ArgsFormales\> \<Bloque\> | \<OperadorAsignacion\> \<ExpresionCompuesta\> ;

\<MetodoInterfaz\> ::= \<TipoMetodo\> idMetVar \<ArgsFormales\> ;

\<TipoMetodo\> ::= \<Tipo\> | void

\<Tipo\> ::= \<TipoBase\> \<DimensionesOpcionales\>

\<TipoBase\> ::= \<TipoPrimitivo\> | \<TipoReferencia\> | idGen

\<DimensionesOpcionales\> ::= \[ \] \<DimensionesOpcionales\> | ϵ

\<TipoReferencia\> ::= idClase \<TipoGenericoOpcional\>

\<TipoPrimitivo\> ::= boolean | char | int

\<TipoGenericoOpcional\> ::= \< \<InstanciadoOParametrico\> \> | ϵ

\<InstanciadoOParametrico\> ::= idGen | idClase \<TipoGenericoOpcional\>

\<ArgsFormales\> ::= ( \<ListaArgsFormalesOpcional\> )

\<ListaArgsFormalesOpcional\> ::= \<ListaArgsFormales\> | ϵ

\<ListaArgsFormales\> ::= \<ArgFormal\> \<ListaArgsFormalesResto\>

\<ListaArgsFormalesResto\> ::= , \<ArgFormal\> \<ListaArgsFormalesResto\> | ϵ

\<ArgFormal\> ::= \<Tipo\> idMetVar

\<Bloque\> ::= { \<ListaSentencias\> }

\<ListaSentencias\> ::= \<Sentencia\> \<ListaSentencias\> | ϵ

\<Sentencia\> ::= ;  
\<Sentencia\> ::= \<VarLocal\> ;  
\<Sentencia\> ::= \<TipoPrimitivo\> \<RestoDeclLocal\>  
\<Sentencia\> ::= idGen \<RestoDeclLocal\>  
\<Sentencia\> ::= idClase \<SentIdClase\>  
\<Sentencia\> ::= \<Return\> ;  
\<Sentencia\> ::= \<If\>  
\<Sentencia\> ::= \<While\>  
\<Sentencia\> ::= \<For\>  
\<Sentencia\> ::= \<Bloque\>  
\<Sentencia\> ::= \<Expresion\> ;      \# sólo si el lookahead ∈ FIRST(\<Expresion\>) \\ { idClase }

\<VarLocal\> ::= var idMetVar \= \<ExpresionCompuesta\>

\<SentIdClase\> ::= \<TipoGenericoOpcional\> \<RestoDeclLocal\>  
\<SentIdClase\> ::= . idMetVar \<ArgsActuales\> \<ReferenciaResto\> \<ExpresionCompuestaResto\> \<RestoAsignacion\> ;

\<RestoDeclLocal\> ::= idMetVar \<MasIdsLocal\> \<InitLocalOpc\> ;

\<MasIdsLocal\> ::= , idMetVar \<MasIdsLocal\> | ϵ

\<For\> ::= for ( \<ClausulasFor\> ) \<Sentencia\>

\<ClausulasFor\> ::= ; \<CondFor\> ; \<ActFor\>  
\<ClausulasFor\> ::= \<TipoPrimitivo\> idMetVar \<TrasIdForTipo\>  
\<ClausulasFor\> ::= idGen idMetVar \<TrasIdForTipo\>  
\<ClausulasFor\> ::= idClase \<TrasIdClaseFor\>  
\<ClausulasFor\> ::= \<Expresion\> ; \<CondFor\> ; \<ActFor\>   \# sólo con FIRST(\<Expresion\>) \\ { idClase }

\<TrasIdForTipo\> ::= : \<Expresion\>  
\<TrasIdForTipo\> ::= \<InitLocalOpc\> ; \<CondFor\> ; \<ActFor\>

\<TrasIdClaseFor\> ::= \<TipoGenericoOpcional\> idMetVar \<TrasIdForTipo\>  
\<TrasIdClaseFor\> ::= . idMetVar \<ArgsActuales\> \<ReferenciaResto\> \<ExpresionCompuestaResto\> \<RestoAsignacion\> ; \<CondFor\> ; \<ActFor\>

\<CondFor\> ::= \<Expresion\> | ϵ  
\<ActFor\>  ::= \<Expresion\> | ϵ

\<InitLocalOpc\> ::= \<OperadorAsignacion\> \<ExpresionCompuesta\> | ϵ

\<Return\> ::= return \<ExpresionOpcional\>

\<ExpresionOpcional\> ::= \<Expresion\> | ϵ

\<If\> ::= if ( \<Expresion\> ) \<Sentencia\> \<ElseOpcional\>

\<ElseOpcional\> ::= else \<Sentencia\> | ϵ

\<While\> ::= while ( \<Expresion\> ) \<Sentencia\>

\<Expresion\> ::= \<ExpresionCompuesta\> \<RestoAsignacion\>

\<RestoAsignacion\> ::= \<OperadorAsignacion\> \<ExpresionCompuesta\> | ϵ

\<OperadorAsignacion\> ::= \=

\<ExpresionCompuesta\> ::= \<ExpresionBasica\> \<ExpresionCompuestaResto\> \<TernarioOpcional\>

\<ExpresionCompuestaResto\> ::= \<OperadorBinario\> \<ExpresionBasica\> \<ExpresionCompuestaResto\> | ϵ

\<OperadorBinario\> ::= || | && | \== | \!= | \< | \> | \<= | \>= | \+ | \- | \* | / | %

\<TernarioOpcional\> ::= ? \<ExpresionCompuesta\> : \<ExpresionCompuesta\> | ϵ

\<ExpresionBasica\> ::= \<OperadorUnario\> \<Operando\> \<PostfijoOpcional\> | \<Operando\> \<PostfijoOpcional\>

\<PostfijoOpcional\> ::= \++ \<PostfijoOpcional\> | \-- \<PostfijoOpcional\> | ϵ

\<OperadorUnario\> ::= \+ | \- | \!

\<Primitivo\> ::= true | false | intLiteral | charLiteral | null

\<Referencia\> ::= \<Primario\> \<ReferenciaResto\>

\<ReferenciaResto\> ::= . idMetVar \<ArgsActualesOpcional\> \<ReferenciaResto\>  
\<ReferenciaResto\> ::= \[ \<Expresion\> \] \<ReferenciaResto\>  
\<ReferenciaResto\> ::= ϵ

\<ArgsActualesOpcional\> ::= \<ArgsActuales\> | ϵ

\<Primario\> ::= this  
\<Primario\> ::= stringLiteral  
\<Primario\> ::= idMetVar \<ArgsActualesOpcional\>  
\<Primario\> ::= new \<RestoNew\>  
\<Primario\> ::= \<LlamadaMetodoEstatico\>  
\<Primario\> ::= \<ExpresionParentizada\>

\<RestoNew\> ::= \<TipoPrimitivo\> \<DimensionesNew\>  
\<RestoNew\> ::= idGen \<DimensionesNew\>  
\<RestoNew\> ::= idClase \<TipoGenericoOpcionalNew\> \<RestoNewIdClase\>

\<TipoGenericoOpcionalNew\> ::= \< \<DiamanteOTipo\> \> | ϵ

\<DiamanteOTipo\> ::= \<InstanciadoOParametrico\> | ϵ    \# ϵ \= notación diamante "\<\>"

\<RestoNewIdClase\> ::= \<DimensionesNew\> | \<ArgsActuales\>

\<ExpresionParentizada\> ::= ( \<Expresion\> )

\<LlamadaMetodoEstatico\> ::= idClase . idMetVar \<ArgsActuales\>

\<DimensionesNew\> ::= \[ \<TrasCorcheteNew\>

\<TrasCorcheteNew\> ::= \] \<MasCorchetesVaciosNew\> \<InicializadorArreglo\>  
\<TrasCorcheteNew\> ::= \<Expresion\> \] \<DimensionesConTamanioOpc\>

\<MasCorchetesVaciosNew\> ::= \[ \] \<MasCorchetesVaciosNew\> | ϵ

\<InicializadorArreglo\> ::= { \<ListaValoresArregloOpcional\> }

\<ListaValoresArregloOpcional\> ::= \<ListaValoresArreglo\> | ϵ    \# "{ }" vacío también es válido

\<ListaValoresArreglo\> ::= \<ValorArreglo\> \<RestoValoresArreglo\>

\<RestoValoresArreglo\> ::= , \<ValorArreglo\> \<RestoValoresArreglo\> | ϵ

\<ValorArreglo\> ::= \<ExpresionCompuesta\> | \<InicializadorArreglo\>

\<DimensionesConTamanioOpc\> ::= \[ \<Expresion\> \] \<DimensionesConTamanioOpc\> | ϵ

\<ArgsActuales\> ::= ( \<ListaExpsOpcional\> )

\<ListaExpsOpcional\> ::= \<ListaExps\> | ϵ

\<ListaExps\> ::= \<Expresion\> \<RestoListaExps\>

\<RestoListaExps\> ::= , \<Expresion\> \<RestoListaExps\> | ϵ

\<Lambda\> ::= \<ParamsLambda\> \-\> \<Expresion\>

\<ParamsLambda\> ::= ( \<ParamsLambdaEntreParen\> ) | idMetVar

\<ParamsLambdaEntreParen\> ::= \<ListaIdLambda\> | ϵ

\<ListaIdLambda\> ::= idMetVar \<RestoListaIdLambda\>

\<RestoListaIdLambda\> ::= , idMetVar \<RestoListaIdLambda\> | ϵ

\<Operando\> ::= \<Primitivo\>  
\<Operando\> ::= this \<ReferenciaResto\>  
\<Operando\> ::= stringLiteral \<ReferenciaResto\>  
\<Operando\> ::= new \<RestoNew\> \<ReferenciaResto\>  
\<Operando\> ::= \<LlamadaMetodoEstatico\> \<ReferenciaResto\>  
\<Operando\> ::= idMetVar \<TrasId\>  
\<Operando\> ::= ( \<TrasParen\>

\<TrasId\> ::= \-\> \<Expresion\>  
\<TrasId\> ::= \<ArgsActualesOpcional\> \<ReferenciaResto\>

\<TrasParen\> ::= ) \-\> \<Expresion\>  
\<TrasParen\> ::= idMetVar \<TrasParenId\>  
\<TrasParen\> ::= \<Expresion\> ) \<ReferenciaResto\>

\<TrasParenId\> ::= , \<ListaIdLambda\> ) \-\> \<Expresion\>  
\<TrasParenId\> ::= ) \<DecidirTrasCierre\>  
\<TrasParenId\> ::= \<TrasId\> \<PostfijoOpcional\> \<ExpresionCompuestaResto\> \<TernarioOpcional\> \<RestoAsignacion\> ) \<ReferenciaResto\>

\<DecidirTrasCierre\> ::= \-\> \<Expresion\>  
\<DecidirTrasCierre\> ::= \<ReferenciaResto\>