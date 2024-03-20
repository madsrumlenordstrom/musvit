{
  description = "A Nix Flake providing a development environment for Musvit";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-23.11";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = { self, nixpkgs, flake-utils, ... }:
    flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = import nixpkgs { inherit system; };
        packages = with pkgs; [
          sbt
          gnumake
          verilator
          coreboot-toolchain.riscv
          gtkwave
          circt
        ];
      in {
        devShells = {
          default = pkgs.mkShell { name = "musvit"; inherit packages; };
        };
      });
}
