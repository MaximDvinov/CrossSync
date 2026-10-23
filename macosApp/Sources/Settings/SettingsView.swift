import SwiftUI
import CoreImage.CIFilterBuiltins
@preconcurrency import CrossSyncShared

struct SettingsWindow: View {
    let environment: AppEnvironment
    @State private var store: SettingsStore?
    var body: some View {
        Group {
            if let store { SettingsView(state: store.state, onEvent: store.send) }
            else { ProgressView() }
        }
        .navigationTitle("CrossSync Settings")
        .frame(minWidth: 520, idealWidth: 620, minHeight: 520, idealHeight: 760)
        .preferredColorScheme(environment.appearance)
        .onAppear { if store == nil { store = SettingsStore(runtime: environment.runtime) } }
        .onDisappear { store?.close(); store = nil }
    }
}

struct SettingsView: View {
    let state: SettingState
    let onEvent: (SettingsEvent) -> Void
    @State private var name = ""
    @State private var editedCategory: Int64?
    @State private var editingCategory = false
    @State private var pairing = false
    @State private var choosingApplications = false
    @State private var permissionMessage: String?
    @State private var notificationPermission = false
    @State private var deletedCategoryID: Int64?
    @State private var unpairedDevice: DeviceData?

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            VStack(alignment: .leading, spacing: 6) {
                Text("Settings").font(.title2.weight(.semibold))
                Text("Clipboard, notifications and connected devices.").font(.callout).foregroundStyle(.secondary)
            }.padding(.horizontal, 28).padding(.top, 24).padding(.bottom, 8)
            Form {
                general
                clipboard
                quickAccess
                notifications
                categories
                applications
                devices
            }.formStyle(.grouped)
        }.frame(maxWidth: 720, maxHeight: .infinity, alignment: .topLeading)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
        .background(Color(nsColor: .windowBackgroundColor))
        .sheet(isPresented: $pairing, onDismiss: { onEvent(.cancelPairing) }) {
            PairingView(state: state, close: { pairing = false })
                .frame(width: 420, height: 500)
        }
        .sheet(isPresented: $choosingApplications) {
            ExcludedApplicationsView(state: state, onEvent: onEvent, close: { choosingApplications = false })
                .frame(width: 520, height: 500)
        }
        .alert(editedCategory == nil ? "New Category" : "Rename Category", isPresented: $editingCategory) {
            TextField("Category name", text: $name)
            Button(editedCategory == nil ? "Add" : "Save", action: saveCategory)
                .disabled(name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
            Button("Cancel", role: .cancel) { }
        }
        .confirmationDialog("Delete category?", isPresented: Binding(get: { deletedCategoryID != nil }, set: { if !$0 { deletedCategoryID = nil } })) {
            Button("Delete Category", role: .destructive) {
                if let deletedCategoryID { onEvent(.deleteCategory(deletedCategoryID)) }
                deletedCategoryID = nil
            }
        }
        .confirmationDialog("Unpair device?", isPresented: Binding(get: { unpairedDevice != nil }, set: { if !$0 { unpairedDevice = nil } })) {
            Button("Unpair", role: .destructive) {
                if let unpairedDevice { onEvent(.unpair(unpairedDevice.id)) }
                unpairedDevice = nil
            }
        } message: { Text("Pair the device again to resume synchronization.") }
        .alert("System Permissions", isPresented: Binding(get: { permissionMessage != nil }, set: { if !$0 { permissionMessage = nil } })) {
            Button("Open System Settings") {
                onEvent(.openPermissions(notificationPermission))
            }
            Button("OK", role: .cancel) { }
        } message: { Text(permissionMessage ?? "") }
    }

    private var general: some View {
        Section("General") {
            if let error = state.platformError {
                Label(error, systemImage: "exclamationmark.triangle").foregroundStyle(.red)
                Button("Open Login Items") { onEvent(.openLoginItems) }
            }
            Toggle("Launch at login", isOn: Binding(get: { state.launchAtSystemStartup }, set: { onEvent(.autoStart($0)) }))
            Picker("Appearance", selection: Binding(get: { state.themeMode.name }, set: { onEvent(.theme($0)) })) {
                Text("System").tag("SYSTEM")
                Text("Light").tag("LIGHT")
                Text("Dark").tag("DARK")
            }
        }
    }

    private var clipboard: some View {
        Section("Clipboard") {
            Picker("Keep clipboard history", selection: Binding(get: { Int(state.clipboardAutoClearTimeoutDays) }, set: { onEvent(.retention(Int32($0))) })) {
                ForEach([1, 3, 7, 15, 30], id: \.self) { Text("\($0) days").tag($0) }
            }
        }
    }

    private var notifications: some View {
        Section("Notifications") {
            Picker("Keep notification history", selection: Binding(get: { Int(state.notificationHistoryRetentionDays) }, set: { onEvent(.notificationRetention(Int32($0))) })) {
                ForEach([1, 3, 7, 15, 30], id: \.self) { Text("\($0) days").tag($0) }
            }
            Picker("Delivery", selection: Binding(get: { state.desktopNotificationDelivery.name }, set: { onEvent(.delivery($0)) })) {
                Text("Off").tag("OFF")
                Text("System").tag("SYSTEM")
                Text("CrossSync popup").tag("POPUP")
                Text("Both").tag("BOTH")
            }
            Button("Enable system notifications") {
                onEvent(.requestNotifications { notificationPermission = true; permissionMessage = $0 })
            }
        }
    }

    private var quickAccess: some View {
        Section("Quick Access") {
            HStack(spacing: 12) {
                Image(systemName: "keyboard").font(.title).foregroundStyle(.secondary)
                VStack(alignment: .leading, spacing: 4) {
                    Text("Open Quick Access").font(.headline)
                    Text("Find an item and press Return to paste.").font(.caption).foregroundStyle(.secondary)
                }
                Spacer()
                Text("⌘ ⇧ V").font(.title3.monospaced()).padding(10)
                    .background(.quaternary, in: RoundedRectangle(cornerRadius: 8))
            }.padding(.vertical, 8)
            Picker("History size", selection: Binding(get: { Int(state.quickAccessHistorySize) }, set: { onEvent(.quickSize(Int32($0))) })) {
                ForEach([10, 20, 50, 100, 200], id: \.self) { Text("\($0) items").tag($0) }
            }
            Button("Enable automatic paste") {
                notificationPermission = false
                onEvent(.enablePaste)
                permissionMessage = "Allow CrossSync in Privacy & Security → Accessibility to paste into the previous application."
            }
        }
    }

    private var categories: some View {
        Section {
            if state.categories.isEmpty {
                Text("No categories yet. Add one to organize your clipboard history.").foregroundStyle(.secondary)
            }
            ForEach(state.categories, id: \.id) { category in
                HStack {
                    Label(category.name, systemImage: "folder")
                    Spacer()
                    Button("Rename") { editedCategory = category.id; name = category.name; editingCategory = true }
                    Button(role: .destructive) { deletedCategoryID = category.id } label: { Image(systemName: "trash") }
                        .help("Delete category").accessibilityLabel("Delete category \(category.name)")
                }
            }
            Button { editedCategory = nil; name = ""; editingCategory = true } label: { Label("Add Category", systemImage: "plus") }
        } header: { Text("Categories") } footer: {
            Text("Move items to a category from clipboard history using drag and drop or the context menu.")
        }
    }

    private var applications: some View {
        Section {
            LabeledContent("Excluded applications", value: "\(state.excludedApplicationIds.count)")
            Button("Choose Applications…") { choosingApplications = true }
        } header: { Text("Excluded Applications") } footer: {
            Text("Content copied from excluded applications is not saved to clipboard history.")
        }
    }

    private var devices: some View {
        Section {
            if state.devices.isEmpty {
                Text("No paired devices. Pair your Android device to start syncing.").foregroundStyle(.secondary)
            }
            ForEach(state.devices, id: \.id) { device in
                HStack {
                    Label(device.name, systemImage: "iphone")
                    Spacer()
                    Button("Unpair", role: .destructive) { unpairedDevice = device }
                }
            }
            Button { onEvent(.pair); pairing = true } label: { Label("Pair Android Device", systemImage: "plus") }
        } header: { Text("Devices") } footer: { Text("Connect both devices to the same local network.") }
    }

    private func saveCategory() {
        let value = name.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !value.isEmpty else { return }
        if let id = editedCategory { onEvent(.renameCategory(id, value)) } else { onEvent(.addCategory(value)) }
        name = ""; editedCategory = nil
    }
}

private struct ExcludedApplicationsView: View {
    let state: SettingState
    let onEvent: (SettingsEvent) -> Void
    let close: () -> Void
    @State private var search = ""
    private var applications: [Application] {
        state.applications.filter { search.isEmpty || $0.name.localizedCaseInsensitiveContains(search) }
            .sorted { $0.name.localizedStandardCompare($1.name) == .orderedAscending }
    }
    var body: some View {
        VStack(spacing: 0) {
            VStack(alignment: .leading, spacing: 12) {
                Text("Excluded Applications").font(.title2.weight(.semibold))
                Text("Turn on an application to exclude its copied content from history.")
                    .font(.callout).foregroundStyle(.secondary)
                TextField("Search applications", text: $search).textFieldStyle(.roundedBorder)
            }.padding(20)
            Divider()
            if applications.isEmpty {
                ContentUnavailableView("No applications found", systemImage: "app.dashed", description: Text("Try another application name."))
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                List(applications, id: \.id) { app in
                    Toggle(app.name, isOn: Binding(get: { state.excludedApplicationIds.contains(app.id) }, set: { excluded in
                        if excluded { onEvent(.exclude(app.id)) } else { onEvent(.include(app.id)) }
                    }))
                }
            }
            Divider()
            HStack {
                Text("\(state.excludedApplicationIds.count) excluded").font(.caption).foregroundStyle(.secondary)
                Spacer()
                Button("Done", action: close).keyboardShortcut(.defaultAction)
            }.padding()
        }
    }
}

private struct PairingView: View {
    let state: SettingState
    let close: () -> Void
    var body: some View {
        VStack(spacing: 20) {
            Text("Pair Android Device").font(.title2.weight(.semibold))
            if let error = (state.pairingState as? PairingStateError)?.message {
                ContentUnavailableView("Unable to pair", systemImage: "exclamationmark.triangle", description: Text(error))
            } else if (state.pairingState is PairingStateConnected) {
                ContentUnavailableView("Device connected", systemImage: "checkmark.circle", description: Text("Your device is ready to sync with this Mac."))
            } else if let code = (state.pairingState as? PairingStateQrCodeGenerated)?.qrCode, let image = qrImage(code) {
                // QR codes must retain a white quiet zone in both system appearances.
                Image(nsImage: image).interpolation(.none).resizable().scaledToFit()
                    .frame(width: 220, height: 220).padding(12).background(.white)
                    .accessibilityLabel("Pairing QR code")
                VStack(alignment: .leading, spacing: 8) {
                    Label("Connect both devices to the same network.", systemImage: "wifi")
                    Label("Open CrossSync on Android and scan this code.", systemImage: "qrcode.viewfinder")
                }.font(.callout).foregroundStyle(.secondary)
            } else {
                ProgressView("Preparing pairing…").frame(maxWidth: .infinity, maxHeight: .infinity)
            }
            Spacer(minLength: 0)
            HStack {
                Spacer()
                Button((state.pairingState is PairingStateConnected) ? "Done" : "Cancel", action: close).keyboardShortcut(.cancelAction)
            }
        }.padding(24)
    }
    private func qrImage(_ string: String) -> NSImage? {
        let filter = CIFilter.qrCodeGenerator(); filter.message = Data(string.utf8)
        guard let output = filter.outputImage?.transformed(by: CGAffineTransform(scaleX: 8, y: 8)),
              let image = CIContext().createCGImage(output, from: output.extent) else { return nil }
        return NSImage(cgImage: image, size: NSSize(width: output.extent.width, height: output.extent.height))
    }
}
