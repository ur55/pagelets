package org.splink.pagelets

import akka.stream.Materializer
import org.splink.pagelets.Exceptions.NoFallbackException
import play.api.mvc.{AnyContent, Request}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}


trait LeafBuilder {
  def leafBuilderService: LeafBuilderService

  trait LeafBuilderService {
    def build(leaf: Leaf[_, _], args: Seq[Arg], requestId: RequestId, isRoot: Boolean)(
      implicit ec: ExecutionContext, r: Request[AnyContent], m: Materializer): Future[PageletResult]
  }
}

trait LeafBuilderImpl extends LeafBuilder {
  self: LeafTools =>
  override val leafBuilderService = new LeafBuilderService {
    val log = play.api.Logger("LeafBuilder")

    override def build(leaf: Leaf[_, _], args: Seq[Arg], requestId: RequestId, isRoot: Boolean)(
      implicit ec: ExecutionContext, r: Request[AnyContent], m: Materializer) = {

      def execute(id: Symbol, isFallback: Boolean,
                  fnc: Seq[Arg] => Future[PageletResult],
                  fallbackFnc: (Seq[Arg], Throwable) => Future[PageletResult]) = {

        def messageFor(t: Throwable) = if (Option(t.getMessage).isDefined) t.getMessage else "No message"

        val startTime = System.currentTimeMillis()
        val s = if (isFallback) " fallback" else ""
        log.info(s"$requestId Invoke$s pagelet $id")

        Try {
          fnc(args).map { result =>
            log.info(s"$requestId Finish$s pagelet $id took ${System.currentTimeMillis() - startTime}ms")
            result
          }.recoverWith {
            case t: Throwable =>
              log.warn(s"$requestId Exception in async$s pagelet $id '${messageFor(t)}'")
              fallbackFnc(args, t)
          }
        } match {
          case Failure(t) =>
            log.warn(s"$requestId Exception in $s pagelet $id '${messageFor(t)}'")
            fallbackFnc(args, t)
          case Success(result) => result
        }
      }


      val build = leaf.execute(leaf.info, _: Seq[Arg])

      val buildFallback = (a: Seq[Arg]) => leaf.fallback.map(f => leaf.execute(f, a)).getOrElse {
        Future.failed(NoFallbackException(leaf.id))
      }

      execute(leaf.id, isFallback = false, build,
        fallbackFnc = (args, t) =>
          execute(leaf.id, isFallback = true, buildFallback,
            fallbackFnc = (args, t) =>
              if (isRoot) Future.failed(t) else Future.successful(PageletResult.empty)
          ))
    }
  }
}
