package org.cartagena.tool.core.util

import scala.util.{Failure, Success, Try}

sealed trait Process[I, O] {

  def apply(s: LazyList[I]): LazyList[Try[O]] =
    Process.apply(this, s)

  def unit(o: O): Process[I, O] =
    Process.emit(o)

  def map[O2](f: O => O2): Process[I, O2] =
    Process.map(this)(f)

  def flatMap[O2](f: O => Process[I, O2]): Process[I, O2] =
    Process.flatMap(this)(f)

  def drain: Process[I, O] =
    Process drain this

  def onHalt(f: Throwable => Process[I, O]): Process[I, O] =
    Process.onHalt(this)(f)

  def |>[O2](p2: Process[O, O2]): Process[I, O2] =
    pipe(p2)

  def pipe[O2](p2: Process[O, O2]): Process[I, O2] =
    Process.pipe(this, p2)

  def ++(p: Process[I, O]): Process[I, O] =
    append(p)

  def append(p: Process[I, O]): Process[I, O] =
    Process.append(this, p)

}

case class Await[I, O](recv: Option[I] => Process[I, O]) extends Process[I, O]

case class Emit[I, O](head: O, tail: Process[I, O] = Halt[I, O](End)) extends Process[I, O]

case class Reclaim[I, O](i: I, tail: Process[I, O] = Halt[I, O](End)) extends Process[I, O]

case class Halt[I, O](err: Throwable) extends Process[I, O]

case object End extends Exception

case object Kill extends Exception

object Process {

  def apply[I, O](p: Process[I, O], s: LazyList[I]): LazyList[Try[O]] = p match {
    case Await(recv) => s match {
      case h #:: t =>
        `try`(recv(Some(h)))(t)
      case xs =>
        `try`(recv(None))(xs)
    }
    case Emit(h, t) =>
      Success(h) #:: t(s)
    case Reclaim(i, t) => s match {
      case x #:: xs =>
        t(i #:: x #:: xs)
      case _ =>
        t(i #:: LazyList.empty)
    }
    case Halt(End | Kill) =>
      LazyList.empty
    case Halt(err) =>
      Failure(err) #:: LazyList.empty
  }

  def unit[I, O](o: O): Process[I, O] =
    emit(o)

  def await[I, O](recv: Option[I] => Process[I, O]): Process[I, O] =
    Await(recv)

  def emit[I, O](head: O, tail: Process[I, O] = Halt[I, O](End)): Process[I, O] =
    Emit(head, tail)

  def halt[I, O]: Process[I, O] =
    Halt(End)

  def halt[I, O](err: Throwable): Process[I, O] =
    Halt(err)

  def reclaim[I, O](input: I, tail: Process[I, O] = Halt[I, O](End)): Process[I, O] =
    Reclaim(input, tail)

  def liftOne[I, O](f: I => O): Process[I, O] =
    await[I, O] {
      case Some(i) =>
        emit(f(i))
      case None =>
        halt
    }

  def lift[I, O](f: I => O): Process[I, O] =
    repeat(liftOne(f))

  def filter[I](p: I => Boolean): Process[I, I] =
    repeat(
      await[I, I] {
        case Some(i) if p(i) =>
          emit(i)
        case _ =>
          halt
      })

  def take[I](n: Int): Process[I, I] =
    await[I, I] {
      case Some(i) if n > 0 =>
        emit(i, take(n - 1))
      case Some(i) if n == 0 =>
        reclaim(i)
      case _ =>
        halt
    }

  def drop[I](n: Int): Process[I, I] =
    await[I, I] {
      case Some(_) if n > 0 =>
        drop(n - 1)
      case Some(i) if n <= 0 =>
        emit(i, drop(0))
      case _ =>
        halt
    }

  def takeWhile[I](f: I => Boolean): Process[I, I] =
    await[I, I] {
      case Some(i) if f(i) =>
        emit(i, takeWhile(f))
      case Some(i) =>
        reclaim(i)
      case _ =>
        halt
    }

  def dropWhile[I](f: I => Boolean): Process[I, I] =
    await[I, I] {
      case Some(i) if f(i) =>
        dropWhile(f)
      case Some(i) =>
        emit(i, dropWhile[I](_ => false))
      case _ =>
        halt
    }

  def count[I]: Process[I, Int] = {
    def go(c: Int): Process[I, Int] = await[I, Int] {
      case Some(_) =>
        emit(c, go(c + 1))
      case _ =>
        halt
    }

    go(1)
  }

  def exists[I](f: I => Boolean): Process[I, Boolean] =
    await[I, Boolean] {
      case Some(i) if !f(i) =>
        emit(false, exists(f))
      case Some(i) if f(i) =>
        emit(true)
      case _ =>
        halt
    }

  def zipWithIndex[I]: Process[I, (I, Int)] = {
    def go(c: Int): Process[I, (I, Int)] = await[I, (I, Int)] {
      case Some(i) =>
        emit(c).flatMap { index =>
          emit[I, (I, Int)](i -> index, go(c + 1))
        }
      case _ =>
        halt
    }

    go(0)
  }

  private def map[I, O, O2](p: Process[I, O])(f: O => O2): Process[I, O2] =
    p |> lift(f)

  private def flatMap[I, O, O2](p: Process[I, O])(f: O => Process[I, O2]): Process[I, O2] =
    p match {
      case Await(recv) =>
        await(recv andThen (_ flatMap f))
      case Emit(h, t) =>
        f(h) ++ t.flatMap(f)
      case Reclaim(i, t) =>
        reclaim(i, t.flatMap(f))
      case Halt(err) =>
        halt(err)
    }

  private def drain[I, O](p: Process[I, O]): Process[I, O] =
    p match {
      case Await(recv) =>
        await(recv andThen (_.drain))
      case Emit(_, t) =>
        t.drain
      case Reclaim(i, t) =>
        reclaim(i, t.drain)
      case Halt(err) =>
        halt(err)
    }

  private def onHalt[I, O](p: Process[I, O])(f: Throwable => Process[I, O]): Process[I, O] =
    p match {
      case Await(recv) =>
        await(recv andThen (_ onHalt f))
      case Emit(h, t) =>
        emit(h, t onHalt f)
      case Reclaim(i, t) =>
        reclaim(i, t onHalt f)
      case Halt(e) =>
        f(e)
    }

  private def pipe[I, O, O2](p1: Process[I, O], p2: Process[O, O2]): Process[I, O2] = {
    def go(pr1: Process[I, O], pr2: Process[O, O2]): Process[I, O2] =
      pr1 match {
        case Await(recv1) => pr2 match {
          case Await(_) => await[I, O2] {
            case Some(i) =>
              go(recv1(Some(i)), pr2)
            case _ =>
              halt
          }
          case Emit(h2, t2) =>
            emit(h2, go(pr1, t2))
          case Reclaim(_, _) =>
            halt(Kill)
          case Halt(err) =>
            halt(err)
        }

        case Emit(h1, t1) => pr2 match {
          case Await(recv2) =>
            go(t1, recv2(Some(h1)))
          case Emit(h2, t2) =>
            emit(h2, go(t1, t2))
          case Reclaim(_, _) =>
            halt(Kill)
          case Halt(err) =>
            halt(err)
        }

        case Reclaim(i, t1) =>
          reclaim(i, go(t1, pr2))

        case Halt(err) => pr2 match {
          case Emit(h2, _) =>
            emit(h2)
          case _ =>
            halt(err)
        }
      }

    go(p1, p2)
  }

  private def append[I, O](p1: Process[I, O], p2: Process[I, O]): Process[I, O] =
    p1.onHalt {
      case End =>
        p2
      case err =>
        halt(err)
    }

  private def repeat[I, O](p: Process[I, O]): Process[I, O] = {
    def go(pr: Process[I, O]): Process[I, O] =
      pr match {
        case Await(recv) => await {
          case None =>
            recv(None)
          case i =>
            go(recv(i))
        }
        case Emit(h, t) =>
          emit(h, go(t))
        case Reclaim(i, t) =>
          reclaim(i, go(t))
        case Halt(End) =>
          go(p)
        case Halt(err) =>
          halt(err)
      }

    go(p)
  }

  private def `try`[I, O](p: => Process[I, O]): Process[I, O] =
    Try(p) match {
      case Success(res) =>
        res
      case Failure(err) =>
        halt(err)
    }

}
