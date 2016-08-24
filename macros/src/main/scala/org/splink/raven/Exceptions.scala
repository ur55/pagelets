package org.splink.raven

object Exceptions {

  class PageletException(msg: String) extends RuntimeException(msg)

  case class TypeException(msg: String) extends PageletException(msg)

  case class NoFallbackException(id: Symbol) extends PageletException(s"Fallback not defined for $id")


}