const imageInput = document.getElementById('image');
imageInput.addEventListener('change', () => {
  const tooLarge = imageInput.files[0]?.size > 3 * 1024 * 1024;
  imageInput.setCustomValidity(tooLarge ? 'La taille de l’image ne doit pas dépasser 3 Mo.' : '');
  if (tooLarge) imageInput.reportValidity();
});
