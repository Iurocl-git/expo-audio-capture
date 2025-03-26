import ExpoModulesCore

public class ExpoAudioCaptureModule: Module {
  // Each module class must implement the definition function. The definition consists of components
  // that describes the module's functionality and behavior.
  // See https://docs.expo.dev/modules/module-api for more details about available components.
  public func definition() -> ModuleDefinition {
    // Sets the name of the module that JavaScript code will use to refer to the module. Takes a string as an argument.
    // Can be inferred from module's class name, but it's recommended to set it explicitly for clarity.
    // The module will be accessible from `requireNativeModule('ExpoAudioCapture')` in JavaScript.
    Name("ExpoAudioCapture")

    // Sets constant properties on the module. Can take a dictionary or a closure that returns a dictionary.
    Constants([
      "PI": Double.pi
    ])

    // Defines event names that the module can send to JavaScript.
    Events("onFftData")

    // Defines a JavaScript synchronous function that runs the native code on the JavaScript thread.
    Function("startCapture") {
      Log.warn("AudioCapture", "This module is not supported on iOS")
      return nil
    }
    
    Function("stopCapture") {
      Log.warn("AudioCapture", "This module is not supported on iOS")
      return nil
    }
    
    Function("setUdpConfig") { (ip: String, port: Int) in
      Log.warn("AudioCapture", "This module is not supported on iOS")
      return nil
    }
    
    // Функции для работы с событиями
    Function("addFftDataListener") { (listener: @escaping ([String: Any]) -> Void) in
      Log.warn("AudioCapture", "This module is not supported on iOS")
      return nil
    }
    
    Function("removeFftDataListener") { (listener: @escaping ([String: Any]) -> Void) in
      Log.warn("AudioCapture", "This module is not supported on iOS")
      return nil
    }

    // Enables the module to be used as a native view. Definition components that are accepted as part of the
    // view definition: Prop, Events.
    // View(ExpoAudioCaptureView.self) {
    //   // Defines a setter for the `url` prop.
    //   Prop("url") { (view: ExpoAudioCaptureView, url: URL) in
    //     if view.webView.url != url {
    //       view.webView.load(URLRequest(url: url))
    //     }
    //   }

    //   Events("onLoad")
    // }
  }
}
