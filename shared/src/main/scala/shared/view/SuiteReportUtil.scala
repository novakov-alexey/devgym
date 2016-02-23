package shared.view

object SuiteReportUtil {
  //scalatest library specific characters for colors
  val green = "\u001B[32m"
  val close = "\u001B[0m"
  val red = "\u001B[31m"

  val reflectionWrapperPattern = """__wrapper([\w\$]*)"""

  def enhanceReport(report: Option[String]) = {
    report.map { r =>
      s"<p id='errorReport'>${
        replaceMarkers(r, "</span><br/>").replaceAll(reflectionWrapperPattern, "")
      }</p>"
    }.getOrElse("")
  }

  def replaceMarkers(report: String, lineEnd: String = "</span>") = {
    report.replace(close, lineEnd)
      .replace(green, """<span class="green">""")
      .replace(red, """<span class="red">""")
  }
}
