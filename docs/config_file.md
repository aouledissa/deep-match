### Deeplink Configuration Object (`DeeplinkConfig`)

Each item in the `deeplinkSpecs` list is a deep link configuration object with the following properties, corresponding to the fields in your `DeeplinkConfig` Kotlin class:

*   **`name`**: (Required, String)
    *   A unique identifier for this deep link specification. This name will be used to link to specific handlers in your code.
    *   Example: `userProfile`, `productView`

*   **`activity`**: (Required, String)
    *   The fully qualified name of the Android Activity that will be primarily associated with this deep link in the `AndroidManifest.xml`.
    *   Example: `com.example.myapp.MainActivity`, `com.example.myapp.ProductDetailsActivity`

*   **`categories`**: (Optional, List of Strings, defaults to `["DEFAULT"]`)
    *   A list of intent filter categories to be added to the generated `<intent-filter>` in the `AndroidManifest.xml`.
    *   Valid values correspond to `IntentFilterCategory` enum (e.g., `DEFAULT`, `BROWSABLE`). The YAML value should be the string representation of the enum constant.
    *   Example: `categories: [DEFAULT, BROWSABLE]`
    *   If omitted, it defaults to `[DEFAULT]`.

*   **`autoVerify`**: (Optional, Boolean, defaults to `false`)
    *   If set to `true`, the `android:autoVerify="true"` attribute will be added to the generated `<intent-filter>`. This is necessary for Android App Links.
    *   Example: `autoVerify: true`

*   **`scheme`**: (Required, String)
    *   The scheme part of the URI.
    *   Example: `myapp`, `http`, `https`

*   **`host`**: (Required, String)
    *   The host part of the URI.
    *   Example: `user`, `product.example.com`

*   **`pathParams`**: (Optional, List of Param objects)
    *   Defines parameters that are part of the URI's path. Each item in the list is a `Param` object.
    *   **Param Object Structure:**
        *   **`name`**: (Required, String) The name of the path parameter (e.g., `userId`, `itemId`). This is how you'll refer to it in your handler.
        *   **`type`**: (Optional, String) The expected data type of the parameter. If present, this hints at the expected format but the value is always passed as a String to the handler initially. You can use this for documentation or potential future validation features. (e.g., `string`, `integer`, `uuid`).
    *   Example: