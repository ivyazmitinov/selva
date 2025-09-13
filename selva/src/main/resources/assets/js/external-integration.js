function removeFields(fieldId) {
    let orderOfRemovedField = document.getElementById(`${fieldId}-order`).value
    let newFieldContainer = document.getElementById(`${fieldId}-container`);
    newFieldContainer.classList.remove("opacity-100")
    newFieldContainer.classList.add("opacity-0")
    setTimeout(() => {
        newFieldContainer.remove()
        document.querySelectorAll('[id$="-order"]')
            .forEach(orderField => {
                if (orderField.value > orderOfRemovedField) {
                    orderField.value = orderField.value - 1
                }
            })
    }, 200)
}


document.addEventListener("DOMContentLoaded", () => {
    let addNewFieldControl = document.getElementById("add-new-field");

    function addNewField(loseFocus) {
        let newFieldOrder = document.querySelectorAll("#editable-fields .fields-container").length;
        let fieldId = `new-field-${crypto.randomUUID()}`;
        let newFieldsHtml = `
<div class="flex mb-2 fields-container transition-opacity duration-200 ease-in opacity-0" id="${fieldId}-container">
    <div class="mr-2"
         title="Remove a field">
        <svg xmlns="http://www.w3.org/2000/svg"
            id="${fieldId}-remove-button"
            data-testid="new-field-${newFieldOrder}-remove-button"
             fill="none"
             viewBox="0 0 24 24"
             stroke-width="1"
             tabindex="0"
             class=" 
                  size-10
                  stroke-sky-400
                  transition-all
                  transform
                  duration-400
                  hover:rotate-180
                  focus:rotate-180
                  select-none
                  hover:stroke-red-600
                  focus:stroke-red-600
                  focus:outline-0
                  cursor-pointer">
            <path stroke-linecap="round" stroke-linejoin="round"
                  d="M15 12H9m12 0a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z"/>
        </svg>
    </div>
    <div class="grid grid-cols-2 gap-2 w-full">
        <input type="text"
           required
           data-testid="new-field-${newFieldOrder}-label"
           id="${fieldId}-label"
           name="${fieldId}__label"
           class="shadow-sm  text-gray-900 border border-gray-300 sm:text-sm rounded-lg  focus:ring-0 focus:outline-none focus:ring-cyan-600 focus:border-cyan-600 h-9 p-1.5"
           placeholder="Custom field name...">
        <input type="hidden"
               id="${fieldId}-order"
               name="${fieldId}__order"
               value="${newFieldOrder}">
        <select id="${fieldId}-type" name="${fieldId}__type"
            data-testid="new-field-${newFieldOrder}-type"
            class="shadow-sm 
            border 
            border-gray-300
             sm:text-sm
            rounded-lg 
            h-9
            p-1.5 
            text-gray-700 
            focus:border-cyan-600
            focus:ring-0
            focus:outline-0
            cursor-pointer">
            <option selected value="text">Text</option>
            <option value="date">Date</option>
            <option value="file">File</option>
        </select>
    </div>
</div>`;
        document.getElementById("editable-fields").insertAdjacentHTML("beforeend", newFieldsHtml)
        let newFieldContainer = document.getElementById(`${fieldId}-container`);
        setTimeout(() => {
            newFieldContainer.classList.remove("opacity-0")
            newFieldContainer.classList.add("opacity-100")
        }, 1)

        let removeFieldButton = document.getElementById(`${fieldId}-remove-button`);
        removeFieldButton.onclick = () => removeFields(fieldId)
        removeFieldButton.onkeydown = (e) => {
            if (e.key === 'Enter') {
                removeFields(fieldId);
            }
        }
        if (loseFocus) {
            addNewFieldControl.blur()
        }
    }

    addNewFieldControl.onclick = () => {
        addNewField(true)
    }
    addNewFieldControl.onkeydown = (e) => {
        if (e.key === 'Enter') {
            addNewField(false);
        }
    }

    let copyToClipboardElement = document.getElementById("copy-api-key-to-clipboard");
    if (copyToClipboardElement) {
        let copyKeyOk = document.getElementById("copy-api-key-to-clipboard-ok");
        copyToClipboardElement.onclick = () => {
            let apiKey = document.getElementById("api-key").value;
            navigator.clipboard.writeText(apiKey)
            copyToClipboardElement.classList.toggle('hidden')
            copyKeyOk.classList.toggle('hidden')
            setTimeout(() => {
                copyToClipboardElement.classList.toggle('hidden')
                copyKeyOk.classList.toggle('hidden')
            }, 1500)
        }
    }

    document.getElementById("delete-integration-form-submit").onclick = () => {
        let deleteOk = confirm("!!!You are going to irrevocable delete this integration and all related " +
            "profiles within Selva!!!");
        if (deleteOk) {
            document.getElementById("delete-integration-form").submit()
        }
    }
})