import SwiftUI
@preconcurrency import CrossSyncShared

struct ClipboardView: View {
    let state: ClipboardState
    let onEvent: (ClipboardEvent) -> Void
    var limit: Int? = nil
    var paste: (() -> Void)? = nil
    var compact = false
    @State private var selected: Int64?
    @State private var search = ""
    @FocusState private var listFocused: Bool
    @FocusState private var searchFocused: Bool
    @State private var clearing = false
    @State private var copyError: String?
    @State private var copied = false
    @State private var searching = false
    @State private var previewing = false

    private var items: [CopiedDataStable] {
        let query = search.trimmingCharacters(in: .whitespacesAndNewlines)
        let history = state.copiedDataList.filter { item in
            query.isEmpty || NativePresentation.shared.text(item: item).localizedCaseInsensitiveContains(query)
                || (item.application?.name ?? item.applicationId ?? "").localizedCaseInsensitiveContains(query)
        }
        return Array(history.prefix(limit ?? history.count))
    }
    private var selectedItem: CopiedDataStable? { items.first { $0.id == selected } }

    var body: some View {
        VStack(spacing: 0) {
            categoryBar
            if searching || !search.isEmpty { searchField.padding(.horizontal, compact ? 10 : 20).padding(.bottom, 8) }
            history
            if compact {
                Divider()
                HStack {
                    Text(copied ? "Copied to clipboard" : "\(items.count) items").font(.caption).foregroundStyle(.secondary)
                    Spacer()
                    Text(paste == nil ? "↑↓ Select · ↵ Copy" : "↑↓ Select · ↵ Paste")
                        .font(.caption2).foregroundStyle(.secondary)
                }.padding(.horizontal, 10).padding(.vertical, 5)
            } else {
                HStack {
                    Text(copied ? "Copied to clipboard" : "\(items.count) items").font(.caption).foregroundStyle(.secondary)
                    Spacer()
                    Text("Click to copy · Return to copy").font(.caption2).foregroundStyle(.secondary)
                }.padding(.horizontal, 20).padding(.vertical, 6)
            }
        }
        .background(Color(nsColor: .windowBackgroundColor))
        .onChange(of: items.map(\.id), initial: true) { _, ids in
            if !ids.contains(where: { $0 == selected }) {
                selected = ids.first { $0 == state.currentCopiedData?.id } ?? ids.first
            }
        }
        .onChange(of: state.currentCopiedData?.id) { _, id in
            if let id, items.contains(where: { $0.id == id }) { selected = id }
        }
        .onChange(of: selected) { _, _ in copied = false }
        .sheet(isPresented: $previewing) {
            VStack(spacing: 0) {
                HStack { Text("Clipboard Item").font(.headline); Spacer(); Button("Done") { previewing = false } }
                    .padding(14)
                Divider()
                preview
            }.frame(width: 560, height: 560)
        }
        .confirmationDialog("Clear history in this category?", isPresented: $clearing) {
            Button("Clear History", role: .destructive) { onEvent(.clear) }
        } message: { Text("This removes all items in the selected category, including items hidden by search.") }
        .alert("Unable to copy", isPresented: Binding(get: { copyError != nil }, set: { if !$0 { copyError = nil } })) {
            Button("OK", role: .cancel) { }
        } message: { Text(copyError ?? "") }
    }

    private var searchField: some View {
        HStack(spacing: 7) {
            Image(systemName: "magnifyingglass").foregroundStyle(.secondary)
            TextField("Search history", text: $search).textFieldStyle(.plain)
                .focused($searchFocused).task { await Task.yield(); searchFocused = true }
                .accessibilityLabel("Search clipboard history")
            if !search.isEmpty {
                Button { search = "" } label: { Image(systemName: "xmark.circle.fill") }
                    .buttonStyle(.plain).foregroundStyle(.secondary).accessibilityLabel("Clear search")
            }
        }.padding(8).background(.quaternary.opacity(0.5), in: RoundedRectangle(cornerRadius: 8))
    }

    private var categoryBar: some View {
        HStack(spacing: 6) {
            ScrollView(.horizontal) {
                HStack(spacing: 6) {
                    categoryButton("All", id: -1)
                    ForEach(state.categories, id: \.id) { category in
                        categoryButton(category.name, id: category.id)
                            .dropDestination(for: String.self) { values, _ in
                                let ids = values.compactMap(Int64.init)
                                ids.forEach { onEvent(.move($0, category.id)) }
                                return !ids.isEmpty
                            }
                    }
                    categoryButton("Uncategorized", id: -2)
                }.padding(.vertical, 2)
            }.scrollIndicators(.hidden)
            Button {
                searching.toggle()
                listFocused = !searching
                if !searching { search = "" }
            }
                label: { Image(systemName: "magnifyingglass") }
                .buttonStyle(.borderless).help("Search history").accessibilityLabel("Search history")
                .keyboardShortcut("f", modifiers: .command)
            Button { clearing = true } label: {
                if compact { Image(systemName: "trash") } else { Text("Clear All") }
            }.buttonStyle(.bordered).disabled(state.copiedDataList.isEmpty)
                .help("Clear the selected category")
        }.controlSize(compact ? .small : .regular)
            .padding(.horizontal, compact ? 10 : 20).padding(.top, compact ? 4 : 0).padding(.bottom, compact ? 6 : 10)
    }

    private func categoryButton(_ title: String, id: Int64) -> some View {
        let active = (state.selectedCategory?.id ?? -1) == id
        return Button { onEvent(.selectCategory(id)) } label: {
            Text(title).font(.system(size: compact ? 11 : 12, weight: .medium)).lineLimit(1)
                .padding(.horizontal, compact ? 9 : 12).padding(.vertical, compact ? 5 : 7)
                .background(active ? Color.accentColor.opacity(0.15) : Color.secondary.opacity(0.08), in: Capsule())
                .foregroundStyle(active ? Color.accentColor : Color.primary)
        }.buttonStyle(.plain).accessibilityAddTraits(active ? [.isSelected] : [])
    }

    @ViewBuilder private var history: some View {
        if items.isEmpty {
            ContentUnavailableView(search.isEmpty ? "Clipboard history is empty" : "No results",
                systemImage: search.isEmpty ? "doc.on.clipboard" : "magnifyingglass",
                description: Text(search.isEmpty ? "Copy text, images or files to get started." : "Try a different search or category."))
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        } else {
            ScrollViewReader { proxy in
                ScrollView {
                    LazyVStack(spacing: compact ? 7 : 10) {
                        ForEach(items, id: \.id) { item in
                            ClipboardRow(item: item, compact: compact, selected: selected == item.id) {
                                itemActions(item)
                            }
                            .id(item.id).contentShape(Rectangle())
                            .gesture(TapGesture(count: 2).exclusively(before: TapGesture(count: 1)).onEnded { gesture in
                                selected = item.id
                                switch gesture {
                                case .first: copy(item)
                                case .second: copy(item, pasteAfterCopy: false)
                                }
                            })
                            .contextMenu { itemActions(item) }
                            .draggable(String(item.id))
                            .accessibilityAddTraits(selected == item.id ? [.isSelected] : [])
                            .accessibilityAction(named: Text(paste == nil ? "Copy" : "Paste")) { copy(item) }
                        }
                    }.padding(.horizontal, compact ? 10 : 20).padding(.top, 2).padding(.bottom, compact ? 8 : 16)
                }
                .focusable().focused($listFocused).focusEffectDisabled()
                .onAppear { if !searching { listFocused = true } }
                .onKeyPress(.upArrow) { moveSelection(-1); return .handled }
                .onKeyPress(.downArrow) { moveSelection(1); return .handled }
                .onKeyPress(.return) {
                    guard let selectedItem else { return .ignored }
                    copy(selectedItem); return .handled
                }
                .onDeleteCommand { if let selectedItem { onEvent(.delete(selectedItem.id)) } }
                .onChange(of: selected) { _, id in if let id { proxy.scrollTo(id) } }
            }
        }
    }

    private func moveSelection(_ direction: Int) {
        guard !items.isEmpty else { return }
        let index = items.firstIndex { $0.id == selected } ?? 0
        selected = items[min(max(index + direction, 0), items.count - 1)].id
    }

    @ViewBuilder private func itemActions(_ item: CopiedDataStable) -> some View {
        Button(paste == nil ? "Copy" : "Paste") { copy(item) }
        Button("Preview…") { selected = item.id; previewing = true }
        moveMenu(item)
        Divider()
        Button("Delete", role: .destructive) { onEvent(.delete(item.id)) }
    }

    @ViewBuilder private var preview: some View {
        if let item = selectedItem {
            VStack(spacing: 0) {
                HStack(spacing: 12) {
                    Label(clipboardKind(item), systemImage: clipboardSymbol(item)).font(.headline)
                    Spacer()
                    Menu { moveMenu(item); Button("Delete", role: .destructive) { onEvent(.delete(item.id)) } }
                        label: { Image(systemName: "ellipsis.circle") }
                        .menuStyle(.borderlessButton).fixedSize().help("Item actions")
                    copyButton.buttonStyle(.borderedProminent)
                }.padding(20)
                Divider()
                ScrollView {
                    VStack(alignment: .leading, spacing: 20) {
                        if let path = NativePresentation.shared.path(item: item), let image = NSImage(contentsOfFile: path) {
                            Image(nsImage: image).resizable().scaledToFit().frame(maxHeight: 440)
                                .clipShape(RoundedRectangle(cornerRadius: 8))
                                .accessibilityLabel("Copied image")
                        } else {
                            ClipboardText(item: item)
                                .textSelection(.enabled).frame(maxWidth: .infinity, alignment: .leading)
                        }
                    }.padding(24).frame(maxWidth: .infinity, alignment: .leading)
                }.frame(maxWidth: .infinity, maxHeight: .infinity)
                Divider()
                VStack(alignment: .leading, spacing: 8) {
                    LabeledContent("Source", value: item.application?.name ?? item.applicationId ?? "Clipboard")
                    LabeledContent("Copied", value: NativePresentation.shared.date(item: item))
                    if copied { Label("Copied to clipboard", systemImage: "checkmark.circle").foregroundStyle(.secondary) }
                }.font(.caption).foregroundStyle(.secondary).padding(20)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }.background(Color(nsColor: .textBackgroundColor))
        } else {
            ContentUnavailableView("Select an item", systemImage: "doc.text.magnifyingglass",
                description: Text("Preview its content, then copy it when you need it."))
        }
    }

    private var copyButton: some View {
        Button { if let selectedItem { copy(selectedItem) } }
            label: { Label(paste == nil ? "Copy" : "Paste", systemImage: "doc.on.doc") }
            .disabled(selectedItem == nil).help("\(paste == nil ? "Copy" : "Paste") selected item (Return)")
    }

    private func moveMenu(_ item: CopiedDataStable) -> some View {
        Menu("Move to Category") {
            ForEach(state.categories, id: \.id) { category in
                Button(category.name) { onEvent(.move(item.id, category.id)) }
            }
        }.disabled(state.categories.isEmpty)
    }

    private func copy(_ item: CopiedDataStable, pasteAfterCopy: Bool = true) {
        onEvent(.copy(item.id, { error in
            copyError = error
            copied = error == nil
            if error == nil && pasteAfterCopy { paste?() }
        }))
    }
}

struct ClipboardRow<Actions: View>: View {
    let item: CopiedDataStable
    var compact = false
    var selected = false
    @ViewBuilder let actions: () -> Actions
    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            content.padding(compact ? 8 : 12)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(Color(nsColor: .textBackgroundColor), in: RoundedRectangle(cornerRadius: 10))
                .overlay(RoundedRectangle(cornerRadius: 10).strokeBorder(selected ? Color.accentColor : Color.secondary.opacity(0.12), lineWidth: selected ? 2 : 0.5))
            HStack(spacing: 5) {
                HStack(spacing: 3) {
                    ClipboardApplicationIcon(application: item.application)
                    Text(item.application?.name ?? item.applicationId ?? "Clipboard")
                        .lineLimit(1)
                }.padding(.horizontal, 6).padding(.vertical, 2).background(.quaternary, in: Capsule())
                Text((item as? CopiedDataStable.FormattedText)?.mimeType ?? clipboardKind(item)).lineLimit(1)
                    .padding(.horizontal, 6).padding(.vertical, 3).background(.quaternary, in: Capsule())
                Spacer(minLength: 2)
                Text(clipboardDate(item)).lineLimit(1).monospacedDigit().fixedSize()
                    .help(NativePresentation.shared.date(item: item))
                    .padding(.horizontal, 6).padding(.vertical, 3).background(.quaternary, in: Capsule())
                Menu(content: actions) { Image(systemName: "ellipsis") }
                    .menuStyle(.borderlessButton).fixedSize().help("Item actions")
                    .accessibilityLabel("Item actions")
            }.font(.system(size: compact ? 10 : 9)).foregroundStyle(.secondary)
        }
    }

    @ViewBuilder private var content: some View {
        if let imageItem = item as? CopiedDataStable.Image {
            HStack(spacing: 8) {
                if let image = ClipboardPresentation.image(path: imageItem.imagePath) {
                    Image(nsImage: image).resizable().scaledToFit()
                        .frame(maxWidth: .infinity, maxHeight: compact ? 64 : 80, alignment: .leading)
                        .accessibilityLabel("Copied image")
                } else { Label("Image unavailable", systemImage: "photo").foregroundStyle(.secondary) }
            }
        } else if let files = item as? CopiedDataStable.File {
            VStack(alignment: .leading, spacing: 2) {
                ForEach(Array(files.filePaths.prefix(compact ? 2 : 3).enumerated()), id: \.offset) { _, path in
                    Label(URL(fileURLWithPath: path).lastPathComponent, systemImage: "doc")
                        .font(.system(size: 12)).lineLimit(1).help(path)
                }
                if files.filePaths.count > (compact ? 2 : 3) {
                    Text("+\(files.filePaths.count - (compact ? 2 : 3)) files").font(.caption2).foregroundStyle(.secondary)
                }
            }
        } else {
            ClipboardText(item: item, preview: true).lineLimit(compact ? 2 : 4)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
    }
}

struct ClipboardText: View {
    let item: CopiedDataStable
    var preview = false
    @State private var rendered: AttributedString?
    private var contentKey: String {
        if let formatted = item as? CopiedDataStable.FormattedText {
            return "\(preview):\(formatted.mimeType):\(formatted.text)"
        }
        return "\(preview):\(NativePresentation.shared.text(item: item))"
    }
    var body: some View {
        Text(rendered ?? AttributedString(NativePresentation.shared.text(item: item)))
            .font(.system(size: preview ? 12 : 14))
            .allowsHitTesting(!preview)
            .task(id: contentKey) {
                await Task.yield()
                guard !Task.isCancelled else { return }
                let result = ClipboardPresentation.text(item: item, preview: preview)
                guard !Task.isCancelled else { return }
                rendered = result
            }
    }
}

struct ClipboardApplicationIcon: View {
    let application: Application?
    var body: some View {
        Group {
            if let image = ClipboardPresentation.applicationIcon(application) {
                Image(nsImage: image).resizable().scaledToFit()
            } else { Image(systemName: "app").foregroundStyle(.secondary) }
        }.frame(width: 14, height: 14).clipShape(RoundedRectangle(cornerRadius: 3)).accessibilityHidden(true)
    }
}

private func clipboardDate(_ item: CopiedDataStable) -> String {
    // The ViewModel already exposes the copy time in the system's local timezone.
    let local = item.date
    return String(format: "%02d.%02d · %02d:%02d", local.day, local.month.ordinal + 1, local.hour, local.minute)
}

private func clipboardSymbol(_ item: CopiedDataStable) -> String {
    switch NativePresentation.shared.kind(item: item) {
    case "image": "photo"
    case "files": "doc"
    case "html", "rtf": "textformat"
    default: "text.alignleft"
    }
}

private func clipboardKind(_ item: CopiedDataStable) -> String {
    switch NativePresentation.shared.kind(item: item) {
    case "image": "Image"
    case "files": "Files"
    case "html", "rtf": "Formatted text"
    default: "Text"
    }
}
