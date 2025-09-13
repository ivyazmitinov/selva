function toggleFieldInput(fieldId) {

    function toggle(inputToEnable, inputToDisable, setDisabled) {
        if (setDisabled) {
            inputToEnable.disabled = false;
            inputToDisable.disabled = true
        }
        inputToEnable.classList.remove("hidden")
        inputToDisable.classList.add("hidden")
    }

    let regularInput = document.getElementById(fieldId);
    let referenceInput = document.getElementById(fieldId + "__reference");
    let switchToReference = document.getElementById(fieldId + "__switch_to_reference");
    let switchToRaw = document.getElementById(fieldId + "__switch_to_raw");
    if (referenceInput.disabled) {
        toggle(referenceInput, regularInput, true);
        toggle(switchToRaw, switchToReference, false);
    } else {
        toggle(regularInput, referenceInput, true)
        toggle(switchToReference, switchToRaw, false)
    }
}

document.addEventListener("DOMContentLoaded", () => {
    document.getElementById("submit-delete-external-profile-form").onclick = () => {
        let deleteOk = confirm("!!!You are going to irrevocable delete your external profile!!!");
        if (deleteOk) {
            document.getElementById("delete-external-profile-form").submit()
        }
    }
})