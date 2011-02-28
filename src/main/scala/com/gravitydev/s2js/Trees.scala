package com.gravitydev.s2js

trait JsTree

case class JsSourceFile (path:String, name:String, children:List[JsTree]) extends JsTree

case class JsClass (owner:JsTree, name:String, parents:List[JsSelect], constructor:JsConstructor, properties:List[JsProperty], methods:List[JsMethod]) extends JsTree

case class JsModule (owner:JsTree, name:String, properties:List[JsProperty], methods:List[JsMethod], classes:List[JsClass], modules:List[JsModule]) extends JsTree

case class JsMethod (owner:JsTree, name:String, params:List[JsParam], body:JsTree, ret:JsTree) extends JsTree

case class JsConstructor (owner:JsTree, params:List[JsParam], constructorBody:List[JsTree], classBody:List[JsTree]) extends JsTree

case class JsVar (id:String, tpe:JsTree, rhs:JsTree) extends JsTree
case class JsBlock (stats:List[JsTree], expr:JsTree) extends JsTree
case class JsLiteral (value:String, tpe:JsBuiltInType) extends JsTree
case class JsVoid () extends JsTree
case class JsOther (clazz:String, children:List[JsTree]) extends JsTree
case class JsProperty (owner:JsTree, name:String, tpt:JsTree, rhs:JsTree, mods:JsModifiers) extends JsTree

case class JsParam (name:String, tpe:JsTree, default:Option[JsTree]) extends JsTree

/* String for the type will have to do until i can figure out how to get an actual type */
case class JsSelect (qualifier:JsTree, name:String, selectType:JsSelectType.Value, tpe:JsType=null ) extends JsTree
case class JsIdent (name:String, tpe:JsType=null) extends JsTree
case class JsApply (fun:JsTree, params:List[JsTree]) extends JsTree

case class JsType (name:String, typeParams:List[String]=Nil) extends JsTree
trait JsBuiltInType
object JsType {
	object IntT extends JsType("Int") with JsBuiltInType
	object StringT extends JsType("String") with JsBuiltInType
	object ArrayT extends JsType("Array") with JsBuiltInType
	object ObjectT extends JsType("Object") with JsBuiltInType
	object BooleanT extends JsType("Boolean") with JsBuiltInType
	object NumberT extends JsType("Number") with JsBuiltInType
	object FunctionT extends JsType("Function") with JsBuiltInType
	object UnknownT extends JsType("UNKOWN") with JsBuiltInType // probably not built-in?
	object AnyT extends JsType("Any") with JsBuiltInType		// probably not built-in?
}

case class JsThis () extends JsTree
case class JsIf (cond:JsTree, thenp:JsTree, elsep:JsTree) extends JsTree
case class JsTernary (cond:JsTree, thenp:JsTree, elsep:JsTree) extends JsTree
case class JsAssign (lhs:JsTree, rhs:JsTree) extends JsTree
case class JsComparison (lhs:JsTree, operator:String, rhs:JsTree) extends JsTree

case class JsEmpty () extends JsTree

case class JsNew (tpt:JsTree) extends JsTree

case class JsSuper (qualifier:JsSelect) extends JsTree

case class JsTypeApply (fun:JsTree, params:List[JsTree]) extends JsTree

case class JsMap (elements:List[JsMapElement]) extends JsTree

case class JsMapElement(key:String, value:JsTree) extends JsTree

case class JsPredef () extends JsTree

case class JsUnaryOp (subject:JsTree, op:String) extends JsTree

case class JsInfixOp (operand1:JsTree, operand2:JsTree, op:String) extends JsTree

case class JsThrow (expr:JsTree) extends JsTree

case class JsFunction (params:List[JsParam], body:JsTree) extends JsTree

case class JsReturn (expr:JsTree) extends JsTree

case class JsPackage (name:String, children:List[JsTree]) extends JsTree

case class JsArray (elements:List[JsTree]) extends JsTree

case class JsArrayAccess (array:JsTree, index:JsTree) extends JsTree

case class JsCast (subject:JsTree, tpe:JsTree) extends JsTree

case class JsModifiers (
	isPrivate:Boolean
)

//case class JsBuiltInType (t:JsBuiltInType.Value) extends JsTree

object JsSelectType extends Enumeration {
	type JsSelectType = Value
	val Method, ParamAccessor, Prop, Module, Class, Package, Other = Value
}

/*
object JsBuiltInType extends Enumeration {
	type JsBuiltInType = Value
	val AnyT, StringT, BooleanT, NumberT, UnknownT, ArrayT = Value
}
*/
