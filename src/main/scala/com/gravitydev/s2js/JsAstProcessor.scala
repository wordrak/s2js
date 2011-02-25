package com.gravitydev.s2js

import S2JSComponent._

object JsAstProcessor {
	def process (tree:JsTree):JsTree = 
		transformTernaries (
		transform (
			clean (
				removeTypeApplications(
					transformMapsAndLists(
						prepare(tree)
					)
				)
			)
		)
		)
		
	// this must be performed before anything else
	def prepare (tree:JsTree):JsTree = {
		def visit[T <: JsTree] (t:JsTree):T = JsAstUtil.visitAst(t, prepare).asInstanceOf[T]
		
		tree match {
			// remove packages
			case JsSourceFile (path, name, packages) => visit {
				def flatten (tree:JsTree) : List[JsTree] = tree match {
					case JsPackage(name, children) => {
						children flatMap flatten
					}
					case x => {
						List(x)
					}
				}
				
				JsSourceFile(
					path,
					name,
					packages flatMap flatten
				)
			}
			
			// collapse application of package methods
			// TODO: come up with better way to identify package objects that doesn't rely on strings, probably with symbol.isPackageObject
			case JsSelect(JsSelect( q, "package", JsSelectType.Module, _), name, t, tpe) => visit {
				JsSelect(q, name, t, tpe)
			}
			// collapse definition of package methods
			case JsSelect(JsSelect(q, name, t, tpe), "package", JsSelectType.Module, _) => visit [JsTree] {
				JsSelect(q, name, t, tpe)
			}
			case JsModule(JsSelect(q, name, JsSelectType.Module, _), "package", props, methods, classes, modules) => visit {
				JsModule(q, name, props, methods, classes, modules)
			}
			
			// remove extra select on instantiations
			case JsSelect(JsNew(tpe), name, t, _) => visit {
				JsNew(tpe)
			}
			
			// collapse predefs selections
			case JsSelect(JsThis(), "Predef", _, _) => {
				JsPredef()
			}
			case JsSelect(JsIdent("scala",_), "Predef", _, _) => {
				JsPredef()
			}
			
			case x => visit[JsTree](x)
		}
			
	}
	
	// must be done before removing type applications
	def transformMapsAndLists (tree:JsTree):JsTree = {
		def visit[T <: JsTree] (t:JsTree):T = JsAstUtil.visitAst(t, transformMapsAndLists).asInstanceOf[T]
		
		tree match {
			
			/*
			 * Turn:
			 *   Object("test" -> 1, "blah" -> "something")
			 * Into:
			 *   {"test":1, "blah":"something"}
			 */
			case JsApply(
					JsSelect (
						JsSelect(
							JsIdent("browser", _),
							"Object",
							JsSelectType.Module,
							JsType(_,_)
						),
						"apply",
						JsSelectType.Method,
						returnType
					),
					params,
					retType
				) => {
					JsMap(
						params.collect({
							case JsApply( JsTypeApply( JsApply( JsSelect( JsApply( JsTypeApply( JsApply( JsSelect( JsPredef(), "any2ArrowAssoc", _, _), _, _), _), List(JsLiteral(key,_)), _), _, _, _), _, _), _), List(value),_) => {
								JsMapElement(key.stripPrefix("\"").stripSuffix("\""), value)
							}
						})
					)
			}
			
			// lists
			case JsApply ( 
					JsTypeApply( 
						JsApply( 
							JsSelect( 
								JsSelect( 
									JsSelect(
										JsSelect(JsIdent("scala",_), "collection", JsSelectType.Module, _), 
										"immutable", 
										JsSelectType.Module,
										_
									), 
									"List", 
									JsSelectType.Module,
									_
								), 
								"apply", 
								JsSelectType.Method,
								_
							), 
							_,
							_
						), 
						_
					), 
					params,
					_
				) => {
				JsArray(params)
			}
							
			case x => visit[JsTree]{x}
		}
	}
	
	// this must be performed before removing default params invocation
	def removeTypeApplications (tree:JsTree):JsTree = {
		def visit[T <: JsTree] (t:JsTree):T = JsAstUtil.visitAst(t, removeTypeApplications).asInstanceOf[T]
		
		tree match {
			// strange setup here: the inner apply's selector goes with the outer apply's paras
			case JsApply(JsTypeApply (JsApply(s, _, _), _), params, _) => visit {
				JsApply(s, params)
			}
			// remove applications in params, which are not wrapped by an apply
			case JsTypeApply (fun, _) => visit {
				fun
			}
			case x => visit[JsTree](x)
		}
	}
	
	def transformTernaries (tree:JsTree):JsTree = {
		def visit[T <: JsTree] (t:JsTree):T = JsAstUtil.visitAst(t, transformTernaries).asInstanceOf[T]
		
		def jsIfToTernary (jsif:JsIf) = JsTernary(jsif.cond, jsif.thenp, jsif.elsep)
		
		tree match {
						
			// ternary
			// TODO: there are a lot more, should probably do this with any function application
			case JsVar (id, tpe, i @ JsIf(_,_,_)) => visit {
				JsVar(id, tpe, jsIfToTernary(i))
			}
			case JsAssign (lhs, i @ JsIf(_,_,_)) => visit {
				JsAssign (lhs, jsIfToTernary(i))
			}
			case a @ JsApply (fun, params, retType) => visit {
				println("test")
				JsApply(fun, params.map(_ match {
					case i:JsIf => jsIfToTernary(i)
					case x => x
				}), retType)
			}
			
			case x => visit[JsTree](x)
		}
	}
	
	def clean (tree:JsTree):JsTree = {
		def visit[T <: JsTree] (t:JsTree):T = JsAstUtil.visitAst(t, clean).asInstanceOf[T]
		
		tree match {				
	
			// remove default param methods
			case JsClass (owner, name, parents, constructor, properties, methods) => visit {
				JsClass (
					owner, 
					name, 
					parents, 
					constructor, 
					properties,
					methods.filter(!_.name.contains("$default$"))
				)
			}
			case JsModule (owner, name, properties, methods, classes, modules) => visit [JsTree] {
				JsModule (
					owner, 
					name, 
					properties, 
					methods.filter(!_.name.contains("$default$")), 
					classes, 
					modules
				)
			}
			case JsBlock (stats, expr) => visit [JsTree] {
				JsBlock(
					stats.filterNot((m) => m.isInstanceOf[JsMethod] && m.asInstanceOf[JsMethod].name.contains("$default$")),
					expr
				)
			}
			
			// turn method String.length() into property
			case JsApply(JsSelect(l @ JsLiteral(value, JsType.StringT), "length", JsSelectType.Method, _), params,_) => {
				JsSelect(l, "length", JsSelectType.Prop, JsType.IntT)
			}
			case a @ JsApply(JsSelect(JsApply(s @ JsSelect(_,_,_, JsType.StringT),_,_),"length",JsSelectType.Method, JsType.IntT), params,_) if true => visit [JsApply] {
				JsSelect(s, "length", JsSelectType.Prop, JsType.IntT)
			}
			
			// remove default invocations
			case JsApply (s, params,_) => visit[JsApply] {
				JsApply (
					s,
					params.map(visit[JsTree]).filter((p) => {
						p match {
							case JsApply(JsSelect(_, name, _, _), params,_) if name contains "$default$" => false
							case JsIdent(name, _) if name.contains("$default$") => false
							case x => true
						}
					})
				)
			}
			
			case x => visit[JsTree](x)
		}
	}
	
	def transform (tree:JsTree):JsTree = {
		def visit[T <: JsTree] (t:JsTree):T = JsAstUtil.visitAst(t, transform).asInstanceOf[T]
		
		tree match {

			case JsSelect(JsSelect(JsSelect(JsIdent("scala",_), "collection", JsSelectType.Module, _), "immutable", JsSelectType.Module, _), "List", JsSelectType.Class, _) => {
				JsType.ArrayT
			}
			
			// println 
			case JsSelect(JsPredef(), "println", t, _) => visit {
				JsSelect(JsIdent("console"), "log", t)
			}
			
			// mkString on List
			// TODO: anything for right now, will narrow down to list later (not sure how yet)
			case JsApply(JsSelect(q, "mkString", t, _), List(glue),_) => visit {
				JsApply(JsSelect(q, "join", t), List(glue))
			}
			
			
			// unary bang
			case JsApply( JsSelect(qualifier, "unary_$bang", t, _), _,_) => visit {
				JsUnaryOp(qualifier, "!")
			}
			
			// infix ops
			case JsApply( JsSelect(q, name, t, _), args,_) if infixOperatorMap contains name => visit [JsTree] {
				JsInfixOp(q, args(0), infixOperatorMap.get(name).get)
			}
			
			// plain exception
			case JsThrow( JsApply( JsNew( JsSelect( JsSelect( JsIdent("scala",_), "package",_, _ ), "Exception", _, _) ), params,_) ) => visit {
				JsThrow( JsApply( JsNew(JsIdent("Error")), params) )
			}
			
			// comparisons
			case JsApply( JsSelect(qualifier, name, _, _), args,_) if comparisonMap contains name.toString => visit [JsTree] {
				JsComparison(
					qualifier,
					(comparisonMap get name.toString).get,
					args.head
				)
			}
			
			// remove browser package prefix
			case JsSelect( JsIdent("browser",_), name, t, _) => visit {
				JsIdent(name)
			}
			case JsType (name:String, t @ _) if name.startsWith("browser.") => visit {
				JsType (name.stripPrefix("browser."), t)
			}
			
			// scala.Unit
			case JsSelect ( JsIdent("scala",_), "Unit", t, _) => visit {
				JsVoid()
			}
			
			// toString on XML literal
			case JsApply ( JsSelect(_, "toString", JsSelectType.Method, _ ), Nil, _ ) => {
				JsVoid()
			}
			
			// method applications with no parameter list
			// turn them into method applications with empty parameter list
			case JsSelect( s @ JsSelect(_,_,JsSelectType.Method,_), name, t, ret) => visit [JsSelect] {
				JsSelect( JsApply(s, Nil), name, t, ret )
			}
			
			// array access on variables
			case JsApply (JsSelect(id @ JsIdent(a, JsType.ArrayT), "apply", JsSelectType.Method,_), params, _) => visit {
				JsArrayAccess(id, params.head)
			}
			// array access on method returns
			case JsApply( JsSelect( a @ JsApply( JsSelect(_,_,_,JsType.ArrayT),_,_), "apply", JsSelectType.Method, _), params, _) => visit {
				JsArrayAccess(a, params.head)
			}
			// map access on variables 
			case JsApply (JsSelect(id @ JsIdent(a, JsType.ObjectT), "get", JsSelectType.Method,_), params, _) => visit {
				JsArrayAccess(id, params.head)
			}
			// map access on method returns
			case JsApply( JsSelect( a @ JsApply( JsSelect(_,_,_,JsType.ObjectT),_,_), "get", JsSelectType.Method, _), params, _) => visit {
				JsArrayAccess(a, params.head)
			}
			
			// collapse applications on local functions
			case a @ JsApply ( JsSelect(i @ JsIdent(_,JsType.FunctionT), "apply",_,_), params, _ ) => visit[JsApply] {
				JsApply(i, params)
			}
			
			case x => visit[JsTree]{x}
		}
	}
}