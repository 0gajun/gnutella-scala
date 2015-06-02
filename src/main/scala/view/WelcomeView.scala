package view

import scalafx.Includes._
import scalafx.application.JFXApp
import scalafx.application.JFXApp.PrimaryStage
import scalafx.geometry.Insets
import scalafx.scene.Scene
import scalafx.scene.control.Button
import scalafx.scene.layout.VBox

/**
 * Created by Junya on 15/06/02.
 */
object WelcomeView extends JFXApp {

  stage = new PrimaryStage {
    scene = new Scene {
      title = "Welcome to Gnutella with scala!!!"
      content = new VBox {
        children = new Button("Connect other servent") {
          onAction = handle {
            println("connect")
          }
        }
        padding = Insets(top = 24, right = 64, bottom = 24, left = 64)
      }
    }
  }

}
