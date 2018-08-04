package ru.ifmo.ds.gui

import java.awt.BorderLayout

import javax.swing.{JComponent, JLabel, JTabbedPane, SwingConstants}
import ru.ifmo.ds.Database

object JComponentExtensions {
  implicit class DirectHierarchy(val comp: JComponent) extends AnyVal {
    def += (that: String): Unit = util.inSwing {
      comp.add(new JLabel(that))
      comp.validate()
    }
    def += (that: JComponent): Unit = util.inSwing {
      comp.add(that)
      comp.validate()
    }
    def -= (that: JComponent): Unit = util.inSwing {
      comp.remove(that)
      comp.validate()
    }
    def apply(index: Int): JComponent = comp.getComponent(index).asInstanceOf[JComponent]
  }

  implicit class AsGroupWrapper(val comp: JComponent) extends AnyVal {
    private def ensureInnerComponentSupportsKeyValueInterface(): Unit = {
      if (comp.getComponentCount != 1 || !comp.getComponent(0).isInstanceOf[JTabbedPane]) {
        comp.removeAll()
        comp.setLayout(new BorderLayout())
        comp.add(new JTabbedPane(SwingConstants.TOP, JTabbedPane.WRAP_TAB_LAYOUT))
        comp.validate()
      }
    }

    private def getInnerComponent: JTabbedPane = comp.getComponent(0) match {
      case jtp: JTabbedPane => jtp
    }

    def += (pair: (String, JComponent)): Unit = this += (pair._1, pair._2)
    def += (key: String, value: JComponent): Unit = util.inSwing {
      ensureInnerComponentSupportsKeyValueInterface()
      getInnerComponent.add(key, value)
      getInnerComponent.validate()
    }

    def addPlots(db: Database, titlePrefix: String, groupKeys: Seq[String],
      xKey: String, xName: String,
      yKey: String, yName: String,
      seriesKey: String
    ): Unit = {
      import scala.collection.mutable
      val map = new mutable.HashMap[Seq[String], mutable.ArrayBuffer[Database.Entry]]()
      db foreach { e =>
        map.getOrElseUpdate(groupKeys.map(e.apply), new mutable.ArrayBuffer()) += e
      }
      def extractTitle(key: Seq[String]): String = groupKeys.size match {
        case 0 => titlePrefix
        case 1 => titlePrefix + ": " + key.head
        case _ => (groupKeys, key).zipped.map((k, v) => k + "=" + v).mkString(titlePrefix + ": ", ", ", "")
      }
      val titledData = map.toIndexedSeq.map(p => (extractTitle(p._1), Database(p._2 :_*))).sortBy(_._1)
      util.inSwing {
        for ((title, db) <- titledData) {
          val wrapper = new SimpleXChartWrapper(comp.getWidth, comp.getHeight, xName, xKey, yName, yKey)
          wrapper.addDatabase(db, seriesKey)
          this += (title, wrapper.gui)
        }
      }
    }

    def apply(title: String): JComponent = {
      val comp = getInnerComponent
      var i = 0
      val compCount = comp.getTabCount
      var good = -1
      while (i < compCount) {
        if (comp.getTitleAt(i) == title) {
          good = i
        }
        i += 1
      }
      if (good == -1) null else comp.getTabComponentAt(i).asInstanceOf[JComponent]
    }
  }
}
