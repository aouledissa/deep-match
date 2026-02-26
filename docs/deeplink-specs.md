### Deeplink Configuration Object (`DeeplinkConfig`)

Each item in the deeplinkSpecs list is a deep link configuration object with the following properties:

- `name`
    - Type: String
    - Required: Yes
    - Description: Unique identifier for the deeplink spec. Used for generated names.
    - Example:
      ```yaml
      name: "open profile"
      ```

- `activity`
    - Type: String
    - Required: Yes
    - Description: Fully qualified name of the activity eligible to resolve this deeplink.
    - Example:
      ```yaml
      activity: com.example.app.MainActivity
      ```

- `categories`
    - Type: List<String>
    - Required: No (default: [DEFAULT])
    - Description: Intent filter categories (DEFAULT, BROWSABLE, etc.).
    - Example:
      ```yaml
      categories: [DEFAULT, BROWSABLE]
      ```

- `autoVerify`
    - Type: Boolean
    - Required: No (default: false)
    - Description: Enables android:autoVerify="true" for app links.
    - Example:
      ```yaml
      autoVerify: true
      ```

- `scheme`
    - Type: List<String>
    - Required: Yes
    - Description: Allowed URI schemes.
    - Example:
      ```yaml
      scheme: [app, https]
      ```

- `host`
    - Type: List<String>
    - Required: No (default: [])
    - Description: Allowed URI hosts/domains. Leave empty (or omit) for hostless URIs such as app:///profile/123.
    - Example:
      ```yaml
      host: ["example.com", "m.example.com"]
      ```

- `pathParams`
    - Type: List of Param
    - Required: No
    - Description: Ordered path segments/templates. Order is positional and preserved.
    - Example:
      ```yaml
      pathParams:
        - name: profile
        - name: userId
          type: alphanumeric
      ```

- `pathParams[].name`
    - Type: String
    - Required: Yes (per item)
    - Description: Path segment label or typed placeholder key.
    - Example:
      ```yaml
      - name: userId
      ```

- `pathParams[].type`
    - Type: String
    - Required: No
    - Description: Path param value type (numeric, alphanumeric, string).
    - Example:
      ```yaml
      - name: userId
        type: numeric
      ```

- `queryParams`
    - Type: List of Param
    - Required: No
    - Description: Typed query definitions. Matching is order-agnostic.
    - Example:
      ```yaml
      queryParams:
        - name: query
          type: string
          required: true
        - name: ref
          type: string
      ```

- `queryParams[].name`
    - Type: String
    - Required: Yes (per item)
    - Description: Query key name.
    - Example:
      ```yaml
      - name: page
      ```

- `queryParams[].type`
    - Type: String
    - Required: Recommended
    - Description: Query value type used for runtime validation and typed generation.
    - Example:
      ```yaml
      - name: ref
        type: string
      ```

- `queryParams[].required`
    - Type: Boolean
    - Required: No (default: false)
    - Description: If true, key must be present to match. Optional typed query params are generated nullable.
    - Example:
      ```yaml
      - name: query
        type: string
        required: true
      ```

- `fragment`
    - Type: String
    - Required: No
    - Description: Required URI fragment (#...). Exposed in generated params when declared.
    - Example:
      ```yaml
      fragment: "details"
      ```

- `description`
    - Type: String
    - Required: No
    - Description: Free-form description for the deeplink spec.
    - Example:
      ```yaml
      description: "Navigate to profile screen from campaign links"
      ```

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
      - name: query
        type: string
        required: true
      - name: ref
        type: string
    fragment: "details"
```

### Tips

- Keep `name` values unique per spec to simplify generated type naming and runtime routing.
- Regenerate sources (`./gradlew generate<Variant>DeeplinkSpecs`) whenever you modify the YAML schema.
- If `generateManifestFiles` is disabled, remember to replicate the `<intent-filter>` changes manually in your main manifest.
- The plugin creates a `<Name>DeeplinkParams` class for every deeplink spec. This avoids ambiguity between "no match" and "matched static deeplink".
- Typed query params are validated by key and type after structural URI matching, so query order does not affect matching.
- Query params are optional by default; set `required: true` only for values that must be present.
- Scheme and host matching are case-insensitive, so values like `HTTPS://Example.COM/...` still match `scheme: [https]` and `host: ["example.com"]`.
- `scheme` must contain at least one value. `host` can be omitted (or set to `[]`) for hostless URIs.
- All generated params classes implement a module-level sealed interface named from the module name (for example, module `app` -> `AppDeeplinkParams`), enabling exhaustive `when` checks.
- The plugin also generates a module-level processor object named from the module name (for example, module `app` -> `AppDeeplinkProcessor`) preloaded with all generated specs.
