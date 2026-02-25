### Deeplink Configuration Object (`DeeplinkConfig`)

Each item in the `deeplinkSpecs` list is a deep link configuration object with the following properties, corresponding to the fields in your `DeeplinkConfig` Kotlin class:

*   **`name`**: (Required, String)
    *   A unique identifier for this deep link specification. This name is used to generate stable spec/params types.
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

*   **`scheme`**: (Required, List of Strings)
    *   One or more URI schemes supported by the deeplink.
    *   Example: `scheme: [myapp, https]`

*   **`host`**: (Required, List of Strings)
    *   One or more hosts (domains) that should resolve to this deeplink.
    *   Example: `host: ["example.com", "m.example.com"]`

*   **`pathParams`**: (Optional, List of Param objects)
    *   Defines ordered parameters that are part of the URI path. Each item in the list is a `Param` object.
    *   **Param Object Structure:**
        *   **`name`**: (Required, String) The name of the path parameter (e.g., `userId`, `itemId`). This is how you'll refer to it in generated params.
        *   **`type`**: (Optional, String) The expected data type of the parameter. When provided, the generated matcher validates the segment against the type’s regex and the runtime converts the value to the corresponding Kotlin type.
    *   Example:
        ```yaml
        pathParams:
          - name: user
          - name: userId
            type: alphanumeric
        ```

*   **`queryParams`**: (Optional, List of Param objects)
    *   Mirrors the structure of `pathParams` but for query string parameters.
    *   Query params should declare a `type` so the generated regex and parameter class enforce the expected format and type conversion.
    *   Example:
        ```yaml
        queryParams:
          - name: ref
            type: string
          - name: page
            type: numeric
        ```

*   **`fragment`**: (Optional, String)
    *   Adds a fragment requirement (`#details`). When provided, the generated parameter class exposes it as a property.

*   **`description`**: (Optional, String)
    *   Free-form text to describe the deeplink’s purpose. Currently informational only.

### Complete Example

```yaml
deeplinkSpecs:
  - name: "open profile"
    description: "Navigate to a user profile page"
    activity: com.example.app.MainActivity
    autoVerify: true
    categories: [DEFAULT, BROWSABLE]
    scheme: [https, app]
    host: ["example.com"]
    pathParams:
      - name: users
      - name: userId
        type: alphanumeric
    queryParams:
      - name: ref
        type: string
    fragment: "details"
```

### Tips

- Keep `name` values unique per spec to simplify generated type naming and runtime routing.
- Regenerate sources (`./gradlew generate<Variant>DeeplinkSpecs`) whenever you modify the YAML schema.
- If `generateManifestFiles` is disabled, remember to replicate the `<intent-filter>` changes manually in your main manifest.
- When a deeplink declares typed path, query, or fragment values, the plugin also creates a `<Name>DeeplinkParams` class so your app receives strongly typed arguments after calling `match(uri)`.
