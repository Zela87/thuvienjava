function createPetals() {
  const petalCount = 30;
  for (let i = 0; i < petalCount; i++) {
    setTimeout(() => {
      const petal = document.createElement("div");
      petal.className = "petal";
      petal.style.left = Math.random() * 100 + "%";
      const pinks = ["#ffc0cb", "#ffb7b2", "#ff9ee5", "#ffdae9"];
      petal.style.background = pinks[Math.floor(Math.random() * pinks.length)];
      petal.style.animationDuration = Math.random() * 3 + 2 + "s";
      document.body.appendChild(petal);
      setTimeout(() => {
        petal.remove();
      }, 5000);
    }, i * 300);
  }
}
window.addEventListener("load", createPetals);
