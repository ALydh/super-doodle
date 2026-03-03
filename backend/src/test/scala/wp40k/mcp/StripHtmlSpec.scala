package wp40k.mcp

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class StripHtmlSpec extends AnyFlatSpec with Matchers {

  "stripHtml" should "remove span tags with classes" in {
    stripHtml("""<span class="kwb">TYRANIDS</span> model only.""") shouldBe "TYRANIDS model only."
  }

  it should "convert br tags to spaces" in {
    stripHtml("Line one.<br>Line two.") shouldBe "Line one. Line two."
  }

  it should "convert list items" in {
    stripHtml("<ul><li>Item 1</li><li>Item 2</li></ul>") shouldBe "Item 1 Item 2"
  }

  it should "strip nested formatting tags" in {
    stripHtml("""<div class="BreakInsideAvoid"><table><tbody><tr><td><p class="impact18">Swarming Instincts</p><p>Each time a model makes an attack.</p></td></tr></tbody></table></div>""") shouldBe "Swarming Instincts Each time a model makes an attack."
  }

  it should "pass through plain text unchanged" in {
    stripHtml("No HTML here") shouldBe "No HTML here"
  }

  it should "handle empty string" in {
    stripHtml("") shouldBe ""
  }

  "stripHtmlOpt" should "strip HTML from Some values" in {
    stripHtmlOpt(Some("<b>Bold</b> text")) shouldBe Some("Bold text")
  }

  it should "pass through None" in {
    stripHtmlOpt(None) shouldBe None
  }
}
