
/*
 * Copyright 2001-2013 Artima, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.scalactic

import org.scalatest._
import prop.GeneratorDrivenPropertyChecks._
import algebra._

class OptionLawsSpec extends UnitSpec with CheckedEquality {

  class OptionFunctorProxy[T](underlying: Option[T]) extends FunctorProxy[Option, T] {
    def map[U](f: T => U): Option[U]  = underlying.map(f)
  }
  implicit object OptionFunctor extends Functor[Option] {
    def apply[T](opt: Option[T]): FunctorProxy[Option, T] = new OptionFunctorProxy[T](opt)
  }
  "Option's map method" should "obey the functor laws" in {
    def id[T] = (o: T) => o
    forAll { opt: Option[Int] =>
      opt.map(id) shouldEqual opt
    }
    forAll { (opt: Option[Int], f: Int => Int, g: Int => Int) =>
      (opt.map(g)).map(f) shouldEqual opt.map(f compose g)
    }
  }
  "A FunctorProxy[Option, T]'s map method" should "obey the functor laws" in {
    def id[T] = (o: T) => o
    forAll { opt: Option[Int] =>
      OptionFunctor(opt).map(id) shouldEqual opt
    }
    forAll { (opt: Option[Int], f: String => String, g: Int => String) =>
      OptionFunctor((OptionFunctor(opt).map(g))).map(f) shouldEqual OptionFunctor(opt).map(f compose g)
    }
  }

  import scala.language.higherKinds
  def id[T] = (o: T) => o
  def identity[Context[_], T](o: Context[T])(implicit functor: Functor[Context]): Unit =
    functor(o).map(id) shouldEqual o

  def composite[Context[_], T, U, V](o: Context[T], f: T => U, g: U => V)(implicit functor: Functor[Context]): Unit =
    functor((functor(o).map(f))).map(g) shouldEqual functor(o).map(g compose f) // g(f(x))

  "Option" should "obey the functor laws via its map method" in {
    forAll { (opt: Option[Int]) => identity(opt) }
    forAll { (opt: Option[Int], f: Int => String, g: String => String) => composite(opt, f, g) }
  }

  import org.scalacheck.Arbitrary
  import org.scalacheck.Shrink
  def assertObeysTheFunctorLaws[Context[_]](implicit arbContextInt: Arbitrary[Context[Int]], shrContextInt: Shrink[Context[Int]], arbIntToString: Arbitrary[Int => String], shrIntToString: Shrink[Int => String], arbStringToChar: Arbitrary[String => Char], shrStringToChar: Shrink[Context[String => Char]], functor: Functor[Context]): Unit = {
    forAll { (opt: Context[Int]) => identity(opt) }
    forAll { (opt: Context[Int], f: Int => String, g: String => Char) => composite(opt, f, g) }
  }

  it should "obey the functor laws via its map method more generically" in {
    assertObeysTheFunctorLaws[Option]
  }
  "Or" should "obey the functor laws via its badMap method" in {
    trait OrWithGood[G] {
      type AndBad[B] = G Or B
    }
    class BadOrFunctorProxy[G, B](underlying: G Or B) extends FunctorProxy[OrWithGood[G]#AndBad, B] {
      def map[C](f: B => C): G Or C  = underlying.badMap(f)
    }
    implicit def badOrFunctor[G]: Functor[OrWithGood[G]#AndBad] =
      new Functor[OrWithGood[G]#AndBad] {
        def apply[B](opt: G Or B): FunctorProxy[OrWithGood[G]#AndBad, B] = new BadOrFunctorProxy[G, B](opt)
      }
    import org.scalacheck.Gen
    implicit def orArb[G, B](implicit arbG: Arbitrary[G], arbB: Arbitrary[B]): Arbitrary[G Or B] =
      Arbitrary(
        for (either <- Arbitrary.arbEither[B, G].arbitrary) yield Or.from(either)
      )
    assertObeysTheFunctorLaws[OrWithGood[Int]#AndBad]

    // instancesOf[OrWithGood[Int]#AndBad] shouldObey theFunctorLaws
    // instancesOf[OrWithGood[Int]#AndBad] should obey the functorLaws
  }
}

