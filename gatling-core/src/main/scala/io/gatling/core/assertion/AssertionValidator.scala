/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.core.assertion

import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.result.{ GroupStatsPath, RequestStatsPath, StatsPath }
import io.gatling.core.result.message.{ KO, OK, Status }
import io.gatling.core.result.reader.{ GeneralStats, DataReader }
import io.gatling.core.validation._

class AssertionValidator(implicit configuration: GatlingConfiguration) {

  type ValidatedRequestPath = Validation[Option[Status] => List[GeneralStats]]
  type StatsByStatus = Option[Status] => List[GeneralStats]

  def validateAssertions(dataReader: DataReader): List[AssertionResult] =
    dataReader.assertions.map(validateAssertion(_, dataReader))

  private def validateAssertion(assertion: Assertion, dataReader: DataReader) = {

    val printablePath = assertion.path.printable(configuration)

    assertion.path match {
      case Global =>
        resolveTarget(assertion, status => List(dataReader.requestGeneralStats(None, None, status)), printablePath)

      case ForAll =>
        val paths = dataReader.statsPaths.collect { case path: RequestStatsPath => path }
        val statsList: StatsByStatus = status => paths.map {
          case RequestStatsPath(request, group) =>
            dataReader.requestGeneralStats(Some(request), group, status)
        }
        resolveTarget(assertion, statsList, printablePath)

      case Details(parts) if parts.isEmpty =>
        resolveTarget(assertion, status => List(dataReader.requestGeneralStats(None, None, status)), printablePath)

      case Details(parts) =>
        val generalStats: ValidatedRequestPath = findPath(parts, dataReader) match {
          case None =>
            Failure(s"Could not find stats matching assertion path $parts")

          case Some(RequestStatsPath(request, group)) =>
            Success(status => List(dataReader.requestGeneralStats(Some(request), group, status)))

          case Some(GroupStatsPath(group)) =>
            Success(status => List(dataReader.groupCumulatedResponseTimeGeneralStats(group, status)))
        }
        generalStats match {
          case Success(stats) => resolveTarget(assertion, stats, printablePath)
          case Failure(msg)   => AssertionResult(assertion, result = false, msg, Nil)
        }
    }
  }

  private def findPath(parts: List[String], dataReader: DataReader): Option[StatsPath] =
    dataReader.statsPaths.find { statsPath =>
      val path = statsPath match {
        case RequestStatsPath(request, group) =>
          group.map(_.hierarchy :+ request).getOrElse(List(request))

        case GroupStatsPath(group) =>
          group.hierarchy
      }
      path == parts
    }

  private def resolveTarget(assertion: Assertion, stats: StatsByStatus, path: String) = {

    val printableTarget = assertion.target.printable(configuration)

    val realValues = assertion.target match {
      case MeanRequestsPerSecondTarget => stats(None).map(_.meanRequestsPerSec.toInt)
      case target: CountTarget         => resolveCountTargetRealValues(target, stats)
      case target: TimeTarget          => resolveTimeTargetRealValues(target, stats)
    }

    resolveCondition(assertion, path, printableTarget, realValues)
  }

  private def resolveCountTargetRealValues(target: CountTarget, stats: StatsByStatus): List[Int] = {

    val resolvedStats = target.metric match {
      case AllRequests        => stats(None)
      case FailedRequests     => stats(Some(KO))
      case SuccessfulRequests => stats(Some(OK))
    }

    target.selection match {
      case Count => resolvedStats.map(_.count)
      case Percent =>
        val metricCountsAndAllCounts = resolvedStats.map(_.count).zip(stats(None).map(_.count))
        val percentages = metricCountsAndAllCounts.map { case (metricCount, allCount) => metricCount.toDouble / allCount * 100 }
        percentages.map(_.toInt)
    }
  }

  private def resolveTimeTargetRealValues(target: TimeTarget, stats: StatsByStatus): List[Int] = {

    val resolvedStats = target.metric match {
      case ResponseTime => stats(None)
    }

    target.selection match {
      case Min               => resolvedStats.map(_.min)
      case Max               => resolvedStats.map(_.max)
      case Mean              => resolvedStats.map(_.mean)
      case StandardDeviation => resolvedStats.map(_.stdDev)
      case Percentiles1      => resolvedStats.map(_.percentile1)
      case Percentiles2      => resolvedStats.map(_.percentile2)
      case Percentiles3      => resolvedStats.map(_.percentile3)
      case Percentiles4      => resolvedStats.map(_.percentile4)
    }
  }

  private def resolveCondition(assertion: Assertion, path: String, printableTarget: String, realValues: List[Int]) = {

    val printableCondition = assertion.condition.printable(configuration)

      def assertionResult(result: Boolean, expectedValueMessage: Any) =
        AssertionResult(assertion, result, s"$path: $printableTarget $printableCondition $expectedValueMessage", realValues)

    assertion.condition match {
      case LessThan(upper)       => assertionResult(realValues.forall(_ <= upper), upper)
      case GreaterThan(lower)    => assertionResult(realValues.forall(_ >= lower), lower)
      case Is(exactValue)        => assertionResult(realValues.forall(_ == exactValue), exactValue)
      case Between(lower, upper) => assertionResult(realValues.forall(v => lower <= v && v <= upper), s"$lower and $upper")
      case In(elements)          => assertionResult(realValues.forall(elements contains), elements)
    }
  }
}
