package controllers

import javax.inject._

import akka.stream.Materializer
import org.splink.raven.FunctionMacros._
import org.splink.raven.BrickResult._
import org.splink.raven.TwirlConversions._
import org.splink.raven._
import play.api.Environment
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc._
import play.twirl.api.{HtmlFormat, Html}
import views.html.wrapper

import scala.concurrent.Future

@Singleton
class HomeController2 @Inject()(implicit m: Materializer, e: Environment) extends Controller with BricksController {

  val plan = Tree('root, Seq(
    Tree('first, Seq(
      Leaf('brick1, pagelet1 _).withFallback(fallbackPagelet _),
      Tree('sub, Seq(
        Leaf('brick2, pagelet2 _),
        Leaf('more, more _)
      ))
    ), results => combine(results)(views.html.test.apply)
    )))

  /*.
      skip(Pagelet2).
      replace(Pagelet1, Leaf(Pagelet2, pagelet2 _)).
      replace(Root, Leaf(Pagelet1, newRoot _))
      */
//TODO error template
  //TODO withJavascriptTop

  val template = wrapper(routes.HomeController2.resourceFor) _

  def index = Wall(template)("Index", plan, Arg("s", "Hello!"))

  def part(id: String) = WallPart(template)(plan, id)

  def pagelet1 = Action.async { implicit request =>
    Future {
      throw new RuntimeException("Oh, an error!")
    }
  }

  def pagelet2(s: String) = Action { implicit request =>
    Ok(s)
    throw new RuntimeException("Ups")
  }

  def fallbackPagelet = Action { implicit request =>
    Ok("<b>fallback!</b>").
      withJavascript(Javascript("hello.js"), Javascript("not found")).
      withCss(Css("hello.css")).
      withCookies(Cookie("yo", "man2")).
      withMetaTags(MetaTag("one", "oneContent"), MetaTag("two", "twoContent"))
  }

  def more = Action { implicit request =>
    Ok(views.html.simple("more...")).
      withCss(Css("hello.css")).
      withMetaTags(MetaTag("one", "oneContent"), MetaTag("three", "threeContent"))
  }

  def newRoot = Action {
    Ok("new root!").withCookies(Cookie("yoRoot", "manRoot"))
  }
}